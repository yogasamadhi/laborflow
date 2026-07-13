import { useMutation } from "@tanstack/react-query";
import { LogIn, UserPlus } from "lucide-react";
import { FormEvent, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { postData } from "../../../lib/axios";
import { LoginUser, setLoginUser, setToken } from "../../../lib/auth";
import { Button } from "../../../components/ui/Button";
import { Field, TextInput } from "../../../components/common/Field";

type LoginResponse = LoginUser & {
  token: string;
};

type AuthMode = "login" | "register";

function errorMessage(error: unknown, fallback: string) {
  return error instanceof Error ? error.message : fallback;
}

export function LoginPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const requestedPath = searchParams.get("next");
  const nextPath = requestedPath && requestedPath.startsWith("/") && !requestedPath.startsWith("//") && !requestedPath.startsWith("/login")
    ? requestedPath
    : "/dashboard";
  const [mode, setMode] = useState<AuthMode>("login");
  const [username, setUsername] = useState("");
  const [nickname, setNickname] = useState("");
  const [phone, setPhone] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [formError, setFormError] = useState("");
  const mutation = useMutation({
    mutationFn: () => {
      if (mode === "register") {
        return postData<LoginResponse>("/auth/register", { username, nickname, phone, password, confirmPassword });
      }
      return postData<LoginResponse>("/auth/login", { username, password });
    },
    onSuccess: (data) => {
      setToken(data.token);
      setLoginUser(data);
      navigate(nextPath, { replace: true });
    }
  });

  function submit(event: FormEvent) {
    event.preventDefault();
    setFormError("");
    if (isRegister && password !== confirmPassword) {
      setFormError("两次输入的密码不一致");
      return;
    }
    mutation.mutate();
  }

  function switchMode(nextMode: AuthMode) {
    setMode(nextMode);
    setFormError("");
    mutation.reset();
  }

  const isRegister = mode === "register";
  const reason = searchParams.get("reason");
  const accessMessage = reason === "session-expired"
    ? "登录已过期，请重新登录。"
    : reason === "login-required"
      ? "请先登录后继续访问。"
      : reason === "signed-out"
        ? "你已安全退出。"
        : "";

  return (
    <div className="login-page">
      <form className="login-card grid" onSubmit={submit}>
        <h1 className="page-title">{isRegister ? "注册 LaborFlow" : "登录 LaborFlow"}</h1>
        <p className="muted">建筑劳务经营与薪酬管理平台</p>
        {accessMessage ? <div className="form-error" role="status">{accessMessage}</div> : null}
        <div className="toolbar">
          <Button type="button" variant={mode === "login" ? "primary" : "secondary"} onClick={() => switchMode("login")}>
            <LogIn size={16} />
            登录
          </Button>
          <Button type="button" variant={mode === "register" ? "primary" : "secondary"} onClick={() => switchMode("register")}>
            <UserPlus size={16} />
            注册
          </Button>
        </div>
        <Field label="账号">
          <TextInput value={username} onChange={(event) => setUsername(event.target.value)} minLength={3} maxLength={64} required />
        </Field>
        {isRegister ? (
          <>
            <Field label="姓名/昵称">
              <TextInput value={nickname} onChange={(event) => setNickname(event.target.value)} maxLength={64} />
            </Field>
            <Field label="手机号">
              <TextInput value={phone} onChange={(event) => setPhone(event.target.value)} maxLength={32} />
            </Field>
          </>
        ) : null}
        <Field label="密码">
          <TextInput type="password" value={password} onChange={(event) => setPassword(event.target.value)} minLength={isRegister ? 8 : undefined} maxLength={64} required />
        </Field>
        {isRegister ? (
          <Field label="确认密码">
            <TextInput type="password" value={confirmPassword} onChange={(event) => setConfirmPassword(event.target.value)} minLength={8} maxLength={64} required />
          </Field>
        ) : null}
        {formError || mutation.error ? <div className="badge badge-red">{formError || errorMessage(mutation.error, isRegister ? "注册失败" : "登录失败")}</div> : null}
        <Button type="submit" disabled={mutation.isPending}>
          {isRegister ? <UserPlus size={16} /> : <LogIn size={16} />}
          {mutation.isPending ? "提交中" : isRegister ? "注册并登录" : "登录"}
        </Button>
      </form>
    </div>
  );
}
