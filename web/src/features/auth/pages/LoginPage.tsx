import { useMutation } from "@tanstack/react-query";
import { LockKeyhole } from "lucide-react";
import { FormEvent, useState } from "react";
import { useNavigate } from "react-router-dom";
import { postData } from "../../../lib/axios";
import { LoginUser, setLoginUser, setToken } from "../../../lib/auth";
import { Button } from "../../../components/ui/Button";
import { Field, TextInput } from "../../../components/common/Field";

type LoginResponse = LoginUser & {
  token: string;
};

export function LoginPage() {
  const navigate = useNavigate();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const mutation = useMutation({
    mutationFn: () => postData<LoginResponse>("/auth/login", { username, password }),
    onSuccess: (data) => {
      setToken(data.token);
      setLoginUser(data);
      navigate("/dashboard");
    }
  });

  function submit(event: FormEvent) {
    event.preventDefault();
    mutation.mutate();
  }

  return (
    <div className="login-page">
      <form className="login-card grid" onSubmit={submit}>
        <h1 className="page-title">LaborFlow</h1>
        <p className="muted">建筑劳务经营与薪酬管理平台</p>
        <Field label="账号">
          <TextInput value={username} onChange={(event) => setUsername(event.target.value)} required />
        </Field>
        <Field label="密码">
          <TextInput type="password" value={password} onChange={(event) => setPassword(event.target.value)} required />
        </Field>
        {mutation.error ? <div className="badge badge-red">登录失败，请检查账号密码</div> : null}
        <Button type="submit" disabled={mutation.isPending}>
          <LockKeyhole size={16} />
          登录
        </Button>
      </form>
    </div>
  );
}
