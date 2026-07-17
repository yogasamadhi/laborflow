#!/usr/bin/env python3
"""
LaborFlow local launcher.

Usage:
  python3 run.py              # start PostgreSQL, server, and web
  python3 run.py --no-docker  # skip docker compose
  python3 run.py --skip-install
  python3 run.py --stop       # stop docker compose services
"""

from __future__ import annotations

import argparse
import os
import shutil
import signal
import socket
import subprocess
import sys
import threading
import time
from pathlib import Path
from urllib.parse import urlparse


ROOT = Path(__file__).resolve().parent
WEB_DIR = ROOT / "web"
SERVER_DIR = ROOT / "server"
ENV_FILE = ROOT / ".env"
ENV_EXAMPLE = ROOT / ".env.example"
COMPOSE_DIR = ROOT / "deploy" / "docker-laborflow"

DEFAULT_JAVA_HOME = Path("/Users/h/code/env/jdk-21/Contents/Home")
DEFAULT_MAVEN = Path("/Users/h/code/env/maven-3.9/bin/mvn")

processes: list[subprocess.Popen[str]] = []
managed_ports: set[int] = set()
stopping = False


def log(message: str) -> None:
    print(f"[run] {message}", flush=True)


def fail(message: str) -> None:
    print(f"[run] ERROR: {message}", file=sys.stderr, flush=True)
    stop_children()
    sys.exit(1)


def read_env_file(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    if not path.exists():
        return values
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        values[key.strip()] = value.strip().strip('"').strip("'")
    return values


def merged_env() -> dict[str, str]:
    env = os.environ.copy()
    env.update(read_env_file(ENV_FILE))
    return env


def ensure_env_file() -> None:
    if ENV_FILE.exists():
        return
    if ENV_EXAMPLE.exists():
        shutil.copyfile(ENV_EXAMPLE, ENV_FILE)
        log("created .env from .env.example")
    else:
        ENV_FILE.write_text(
            "\n".join(
                [
                    "POSTGRES_DB=laborflow",
                    "POSTGRES_USER=laborflow",
                    "POSTGRES_PASSWORD=laborflow123",
                    "DATABASE_URL=jdbc:postgresql://localhost:5434/laborflow",
                    "SERVER_PORT=8089",
                    "JWT_SECRET=please-change-this-secret-to-at-least-32-characters",
                    "UPLOAD_DIR=./uploads",
                    "",
                ]
            ),
            encoding="utf-8",
        )
        log("created default .env")


def command_exists(command: str) -> bool:
    return shutil.which(command) is not None


def prepare_cmd(cmd: list[str]) -> list[str]:
    # On Windows, npm/Maven CLIs ship as .cmd shims (pnpm.CMD, mvn.CMD).
    # CreateProcess cannot launch .cmd/.bat directly — only real .exe files —
    # so route through cmd.exe to avoid a spurious "command not found".
    if os.name == "nt":
        return ["cmd", "/c", *cmd]
    return cmd


def run_checked(cmd: list[str], cwd: Path, env: dict[str, str] | None = None) -> None:
    log(f"running: {' '.join(cmd)}")
    try:
        subprocess.run(prepare_cmd(cmd), cwd=cwd, env=env, check=True)
    except FileNotFoundError:
        fail(f"command not found: {cmd[0]}")
    except subprocess.CalledProcessError as exc:
        fail(f"command failed with exit code {exc.returncode}: {' '.join(cmd)}")


def popen_stream(name: str, cmd: list[str], cwd: Path, env: dict[str, str] | None = None) -> subprocess.Popen[str]:
    log(f"starting {name}: {' '.join(cmd)}")
    popen_kwargs: dict[str, object] = {}
    if os.name == "nt":
        popen_kwargs["creationflags"] = subprocess.CREATE_NEW_PROCESS_GROUP
    else:
        popen_kwargs["start_new_session"] = True
    try:
        proc = subprocess.Popen(
            prepare_cmd(cmd),
            cwd=cwd,
            env=env,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            # Force UTF-8: child output (e.g. Vite's ➜) is UTF-8, but Windows text
            # mode defaults to the locale codepage (GBK/cp936) and crashes the pump.
            encoding="utf-8",
            errors="replace",
            bufsize=1,
            **popen_kwargs,
        )
    except FileNotFoundError:
        fail(f"command not found: {cmd[0]}")

    processes.append(proc)

    def pump() -> None:
        assert proc.stdout is not None
        for line in proc.stdout:
            print(f"[{name}] {line}", end="", flush=True)

    threading.Thread(target=pump, daemon=True).start()
    return proc


def stop_children() -> None:
    global stopping
    if stopping:
        return
    stopping = True
    for proc in processes:
        if proc.poll() is None:
            if os.name == "nt":
                # Commands run via cmd.exe (see prepare_cmd); terminating the wrapper
                # would not reach its grandchildren (java/node), so kill the whole tree.
                subprocess.run(
                    ["taskkill", "/F", "/T", "/PID", str(proc.pid)],
                    stdout=subprocess.DEVNULL,
                    stderr=subprocess.DEVNULL,
                )
            else:
                try:
                    os.killpg(proc.pid, signal.SIGTERM)
                except ProcessLookupError:
                    pass
    deadline = time.time() + 8
    for proc in processes:
        if proc.poll() is None:
            remaining = max(0.1, deadline - time.time())
            try:
                proc.wait(timeout=remaining)
            except subprocess.TimeoutExpired:
                if os.name == "nt":
                    subprocess.run(
                        ["taskkill", "/F", "/T", "/PID", str(proc.pid)],
                        stdout=subprocess.DEVNULL,
                        stderr=subprocess.DEVNULL,
                    )
                else:
                    try:
                        os.killpg(proc.pid, signal.SIGKILL)
                    except ProcessLookupError:
                        pass
                try:
                    proc.wait(timeout=1)
                except subprocess.TimeoutExpired:
                    pass
    for port in sorted(managed_ports):
        kill_processes_on_port(f"port {port}", port)


def process_exists(pid: int) -> bool:
    if pid <= 0:
        return False
    if os.name == "nt":
        result = subprocess.run(
            ["tasklist", "/FI", f"PID eq {pid}"],
            stdout=subprocess.PIPE,
            stderr=subprocess.DEVNULL,
            text=True,
            encoding="utf-8",
            errors="replace",
            check=False,
        )
        return str(pid) in result.stdout
    try:
        os.kill(pid, 0)
        return True
    except ProcessLookupError:
        return False
    except PermissionError:
        return True


def find_listening_pids(port: int) -> set[int]:
    pids: set[int] = set()
    if os.name == "nt":
        result = subprocess.run(
            ["netstat", "-ano", "-p", "tcp"],
            stdout=subprocess.PIPE,
            stderr=subprocess.DEVNULL,
            text=True,
            encoding="utf-8",
            errors="replace",
            check=False,
        )
        for line in result.stdout.splitlines():
            parts = line.split()
            if len(parts) < 5 or parts[0].upper() != "TCP" or parts[3].upper() != "LISTENING":
                continue
            local_address = parts[1].rsplit(":", 1)
            if len(local_address) == 2 and local_address[1] == str(port):
                try:
                    pids.add(int(parts[-1]))
                except ValueError:
                    pass
        return pids

    if command_exists("lsof"):
        result = subprocess.run(
            ["lsof", "-nP", f"-tiTCP:{port}", "-sTCP:LISTEN"],
            stdout=subprocess.PIPE,
            stderr=subprocess.DEVNULL,
            text=True,
            encoding="utf-8",
            errors="replace",
            check=False,
        )
        for raw_pid in result.stdout.splitlines():
            try:
                pids.add(int(raw_pid.strip()))
            except ValueError:
                pass
        return pids

    if command_exists("fuser"):
        result = subprocess.run(
            ["fuser", f"{port}/tcp"],
            stdout=subprocess.PIPE,
            stderr=subprocess.DEVNULL,
            text=True,
            encoding="utf-8",
            errors="replace",
            check=False,
        )
        for raw_pid in result.stdout.split():
            try:
                pids.add(int(raw_pid.strip()))
            except ValueError:
                pass
    return pids


def kill_processes_on_port(service: str, port: int) -> bool:
    pids = {pid for pid in find_listening_pids(port) if pid != os.getpid()}
    if not pids:
        return False

    log(f"{service} is using port {port}; stopping PID(s): {', '.join(map(str, sorted(pids)))}")
    for pid in sorted(pids):
        if os.name == "nt":
            subprocess.run(
                ["taskkill", "/T", "/PID", str(pid)],
                stdout=subprocess.DEVNULL,
                stderr=subprocess.DEVNULL,
                check=False,
            )
        else:
            try:
                os.kill(pid, signal.SIGTERM)
            except ProcessLookupError:
                pass

    deadline = time.time() + 5
    while time.time() < deadline:
        if not any(process_exists(pid) for pid in pids):
            return True
        time.sleep(0.2)

    remaining = [pid for pid in sorted(pids) if process_exists(pid)]
    if remaining:
        log(f"force stopping PID(s): {', '.join(map(str, remaining))}")
    for pid in remaining:
        if os.name == "nt":
            subprocess.run(
                ["taskkill", "/F", "/T", "/PID", str(pid)],
                stdout=subprocess.DEVNULL,
                stderr=subprocess.DEVNULL,
                check=False,
            )
        else:
            try:
                os.kill(pid, signal.SIGKILL)
            except ProcessLookupError:
                pass
    return True


def parse_database_host_port(env: dict[str, str]) -> tuple[str, int]:
    url = env.get("DATABASE_URL", "jdbc:postgresql://localhost:5434/laborflow")
    if url.startswith("jdbc:"):
        url = url[5:]
    parsed = urlparse(url)
    return parsed.hostname or "localhost", parsed.port or 5434


def parse_port(env: dict[str, str], name: str, default: int) -> int:
    raw = env.get(name, str(default))
    try:
        port = int(raw)
    except ValueError:
        fail(f"{name} must be a valid TCP port, got: {raw}")
    if port < 1 or port > 65535:
        fail(f"{name} must be between 1 and 65535, got: {raw}")
    return port


def ensure_port_available(service: str, host: str, port: int, env_name: str) -> None:
    try:
        with socket.create_connection((host, port), timeout=1):
            pass
    except OSError:
        return

    kill_processes_on_port(service, port)
    try:
        with socket.create_connection((host, port), timeout=1):
            fail(
                f"{service} port {port} is still in use after cleanup. "
                f"Stop the existing process or set {env_name} to another port."
            )
    except OSError:
        return

def wait_for_port(service: str, host: str, port: int, timeout_seconds: int = 60) -> None:
    log(f"waiting for {service} at {host}:{port}")
    deadline = time.time() + timeout_seconds
    while time.time() < deadline:
        for proc in processes:
            code = proc.poll()
            if code is not None:
                fail(f"a child process exited unexpectedly with code {code}")
        try:
            with socket.create_connection((host, port), timeout=2):
                log(f"{service} is reachable")
                return
        except OSError:
            pass
        time.sleep(1)
    fail(f"{service} is not reachable at {host}:{port}")


def ensure_docker(env: dict[str, str]) -> None:
    if not command_exists("docker"):
        fail("docker is required unless --no-docker is used")
    if not (COMPOSE_DIR / "docker-compose.yml").exists():
        fail(f"docker-compose.yml not found in {COMPOSE_DIR}")
    run_checked(["docker", "compose", "up", "-d"], COMPOSE_DIR, env)


def stop_docker(env: dict[str, str]) -> None:
    if not command_exists("docker"):
        fail("docker command not found")
    if not (COMPOSE_DIR / "docker-compose.yml").exists():
        fail(f"docker-compose.yml not found in {COMPOSE_DIR}")
    run_checked(["docker", "compose", "down"], COMPOSE_DIR, env)


def find_java_home(env: dict[str, str]) -> Path:
    configured = env.get("JAVA_HOME")
    if configured and (Path(configured) / "bin" / "java").exists():
        return Path(configured)
    if DEFAULT_JAVA_HOME.exists():
        return DEFAULT_JAVA_HOME
    java = shutil.which("java")
    if java:
        return Path(java).resolve().parents[1]
    fail("Java 21 not found. Set JAVA_HOME or install JDK 21.")
    raise AssertionError("unreachable")


def find_maven(env: dict[str, str]) -> str:
    configured = env.get("MAVEN_BIN")
    if configured and Path(configured).exists():
        return configured
    if DEFAULT_MAVEN.exists():
        return str(DEFAULT_MAVEN)
    mvn = shutil.which("mvn")
    if mvn:
        return mvn
    fail("Maven not found. Set MAVEN_BIN or install Maven.")
    raise AssertionError("unreachable")


def ensure_web_dependencies(env: dict[str, str]) -> None:
    if not command_exists("pnpm"):
        fail("pnpm is required for the web project")
    run_checked(["pnpm", "install"], WEB_DIR, env)


def start_server(env: dict[str, str]) -> subprocess.Popen[str]:
    java_home = find_java_home(env)
    maven = find_maven(env)
    server_env = env.copy()
    server_env["JAVA_HOME"] = str(java_home)
    server_env["PATH"] = f"{java_home / 'bin'}{os.pathsep}{server_env.get('PATH', '')}"
    return popen_stream(
        "server",
        [maven, "spring-boot:run"],
        SERVER_DIR,
        server_env,
    )


def start_web(env: dict[str, str]) -> subprocess.Popen[str]:
    return popen_stream("web", ["pnpm", "dev"], WEB_DIR, env)


def wait_forever() -> None:
    env = merged_env()
    web_port = env.get("WEB_PORT", "5175")
    server_port = env.get("SERVER_PORT", "8089")
    log(f"started. Web: http://localhost:{web_port}  API: http://localhost:{server_port}")
    log("press Ctrl+C to stop server and web processes")
    while True:
        for proc in processes:
            code = proc.poll()
            if code is not None:
                fail(f"a child process exited unexpectedly with code {code}")
        time.sleep(1)


def main() -> None:
    parser = argparse.ArgumentParser(description="Start LaborFlow local development services.")
    parser.add_argument("--no-docker", action="store_true", help="skip docker compose startup")
    parser.add_argument("--skip-install", action="store_true", help="skip pnpm install")
    parser.add_argument("--stop", action="store_true", help="stop docker compose services and exit")
    args = parser.parse_args()

    ensure_env_file()
    env = merged_env()

    if args.stop:
        stop_docker(env)
        return

    if not args.no_docker:
        ensure_docker(env)

    host, port = parse_database_host_port(env)
    wait_for_port("PostgreSQL", host, port)

    server_port = parse_port(env, "SERVER_PORT", 8089)
    web_port = parse_port(env, "WEB_PORT", 5175)
    managed_ports.update({server_port, web_port})
    ensure_port_available("API", "localhost", server_port, "SERVER_PORT")
    ensure_port_available("Web", "localhost", web_port, "WEB_PORT")

    if not args.skip_install:
        ensure_web_dependencies(env)

    start_server(env)
    start_web(env)
    wait_for_port("API", "localhost", server_port)
    wait_for_port("Web", "localhost", web_port)
    wait_forever()


def shutdown(_sig: int, _frame: object) -> None:
    log("stopping...")
    stop_children()
    sys.exit(0)


if __name__ == "__main__":
    signal.signal(signal.SIGINT, shutdown)
    signal.signal(signal.SIGTERM, shutdown)
    try:
        main()
    finally:
        stop_children()
