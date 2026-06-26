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

DEFAULT_JAVA_HOME = Path("/Users/h/code/env/jdk-21/Contents/Home")
DEFAULT_MAVEN = Path("/Users/h/code/env/maven-3.9/bin/mvn")
DEFAULT_MAVEN_REPO = ROOT / ".m2" / "repository"

processes: list[subprocess.Popen[str]] = []


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


def run_checked(cmd: list[str], cwd: Path, env: dict[str, str] | None = None) -> None:
    log(f"running: {' '.join(cmd)}")
    try:
        subprocess.run(cmd, cwd=cwd, env=env, check=True)
    except FileNotFoundError:
        fail(f"command not found: {cmd[0]}")
    except subprocess.CalledProcessError as exc:
        fail(f"command failed with exit code {exc.returncode}: {' '.join(cmd)}")


def popen_stream(name: str, cmd: list[str], cwd: Path, env: dict[str, str] | None = None) -> subprocess.Popen[str]:
    log(f"starting {name}: {' '.join(cmd)}")
    try:
        proc = subprocess.Popen(
            cmd,
            cwd=cwd,
            env=env,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            bufsize=1,
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
    for proc in processes:
        if proc.poll() is None:
            proc.terminate()
    deadline = time.time() + 8
    for proc in processes:
        if proc.poll() is None:
            remaining = max(0.1, deadline - time.time())
            try:
                proc.wait(timeout=remaining)
            except subprocess.TimeoutExpired:
                proc.kill()


def parse_database_host_port(env: dict[str, str]) -> tuple[str, int]:
    url = env.get("DATABASE_URL", "jdbc:postgresql://localhost:5434/laborflow")
    if url.startswith("jdbc:"):
        url = url[5:]
    parsed = urlparse(url)
    return parsed.hostname or "localhost", parsed.port or 5434


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
    run_checked(["docker", "compose", "up", "-d"], ROOT, env)


def stop_docker(env: dict[str, str]) -> None:
    if not command_exists("docker"):
        fail("docker command not found")
    run_checked(["docker", "compose", "down"], ROOT, env)


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
    DEFAULT_MAVEN_REPO.mkdir(parents=True, exist_ok=True)
    return popen_stream(
        "server",
        [maven, f"-Dmaven.repo.local={DEFAULT_MAVEN_REPO}", "spring-boot:run"],
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

    if not args.skip_install:
        ensure_web_dependencies(env)

    start_server(env)
    start_web(env)
    wait_for_port("API", "localhost", int(env.get("SERVER_PORT", "8089")))
    wait_for_port("Web", "localhost", int(env.get("WEB_PORT", "5175")))
    wait_forever()


if __name__ == "__main__":
    signal.signal(signal.SIGINT, lambda _sig, _frame: (log("stopping..."), stop_children(), sys.exit(0)))
    signal.signal(signal.SIGTERM, lambda _sig, _frame: (log("stopping..."), stop_children(), sys.exit(0)))
    main()
