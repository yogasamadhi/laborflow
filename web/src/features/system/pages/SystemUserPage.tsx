import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Ban, KeyRound, RefreshCw, Save, UserPlus } from "lucide-react";
import { FormEvent, useState } from "react";
import { getAllPages, postData, putData } from "../../../lib/axios";
import { statusText } from "../../../lib/utils";
import { Button } from "../../../components/ui/Button";
import { Field, TextInput } from "../../../components/common/Field";
import { Page } from "../../../components/common/Page";
import { usePagedQuery } from "../../../components/common/usePagedQuery";
import { AsyncLookupSelect } from "../../../components/common/AsyncLookupSelect";

type Role = {
  id: string;
  code: string;
  name: string;
  status: string;
};

type SystemUser = {
  id: string;
  username: string;
  employeeId?: string | null;
  employeeName?: string | null;
  nickname?: string | null;
  phone?: string | null;
  status: string;
  roleIds: string[];
  roles: string[];
  lastLoginAt?: string | null;
  lockedUntil?: string | null;
  passwordUpdatedAt?: string | null;
  createdAt?: string | null;
};

export function SystemUserPage() {
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<SystemUser | null>(null);
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [nickname, setNickname] = useState("");
  const [phone, setPhone] = useState("");
  const [employeeId, setEmployeeId] = useState("");
  const [roleIds, setRoleIds] = useState<string[]>([]);
  const userListing = usePagedQuery<SystemUser>(["system/users"], "/system/users");
  const { data: usersData, isFetching, refetch } = userListing;
  const { data: rolesData } = useQuery({
    queryKey: ["system/roles", "options"],
    queryFn: () => getAllPages<Role>("/system/roles", { status: "active" })
  });
  const users = usersData?.records ?? [];
  const roles = rolesData ?? [];
  const invalidate = () => queryClient.invalidateQueries({ queryKey: ["system/users"] });

  const saveMutation = useMutation({
    mutationFn: () => {
      const payload = {
        username,
        password: editing ? undefined : password,
        nickname,
        phone,
        employeeId: employeeId || undefined,
        roleIds: roleIds.map(Number),
        status: editing?.status ?? "active"
      };
      return editing ? putData(`/system/users/${editing.id}`, payload) : postData("/system/users", payload);
    },
    onSuccess: () => {
      resetForm();
      invalidate();
    }
  });
  const resetPasswordMutation = useMutation({
    mutationFn: () => postData(`/system/users/${editing?.id}/reset-password`, { newPassword: password }),
    onSuccess: () => {
      setPassword("");
      invalidate();
    }
  });
  const disableMutation = useMutation({
    mutationFn: (id: string) => postData(`/system/users/${id}/disable`),
    onSuccess: invalidate
  });

  function submit(event: FormEvent) {
    event.preventDefault();
    saveMutation.mutate();
  }

  function edit(user: SystemUser) {
    setEditing(user);
    setUsername(user.username);
    setPassword("");
    setNickname(user.nickname ?? "");
    setPhone(user.phone ?? "");
    setEmployeeId(user.employeeId ?? "");
    setRoleIds(user.roleIds?.map(String) ?? []);
  }

  function resetForm() {
    setEditing(null);
    setUsername("");
    setPassword("");
    setNickname("");
    setPhone("");
    setEmployeeId("");
    setRoleIds([]);
  }

  function toggleRole(roleId: string) {
    setRoleIds((current) => current.includes(roleId) ? current.filter((id) => id !== roleId) : [...current, roleId]);
  }

  return (
    <Page title="用户管理">
      <form className="panel toolbar" onSubmit={submit}>
        <Field label="账号">
          <TextInput value={username} onChange={(event) => setUsername(event.target.value)} required />
        </Field>
        <Field label={editing ? "新密码" : "初始密码"}>
          <TextInput type="password" value={password} onChange={(event) => setPassword(event.target.value)} required={!editing} />
        </Field>
        <Field label="昵称">
          <TextInput value={nickname} onChange={(event) => setNickname(event.target.value)} />
        </Field>
        <Field label="手机号">
          <TextInput value={phone} onChange={(event) => setPhone(event.target.value)} />
        </Field>
        <Field label="关联员工">
          <AsyncLookupSelect endpoint="/reference/employees" value={employeeId} onChange={setEmployeeId} params={{ status: "active" }} emptyLabel="不关联员工" />
        </Field>
        <Field label="角色">
          <div className="checkbox-chips">
            {roles.map((role) => (
              <label className="checkbox-chip" key={role.id}>
                <input type="checkbox" checked={roleIds.includes(String(role.id))} onChange={() => toggleRole(String(role.id))} />
                <span>{role.name}</span>
              </label>
            ))}
          </div>
        </Field>
        <Button type="submit" disabled={saveMutation.isPending}>
          {editing ? <Save size={16} /> : <UserPlus size={16} />}
          {editing ? "保存" : "新增"}
        </Button>
        <Button type="button" variant="secondary" onClick={() => resetPasswordMutation.mutate()} disabled={!editing || !password || resetPasswordMutation.isPending}>
          <KeyRound size={16} />
          重置密码
        </Button>
        <Button type="button" variant="secondary" onClick={resetForm}>
          <RefreshCw size={16} />
          清空
        </Button>
      </form>
      <div className="panel table-wrap">
        <table>
          <thead>
            <tr>
              <th>账号</th>
              <th>昵称</th>
              <th>员工</th>
              <th>角色</th>
              <th>状态</th>
              <th>最后登录</th>
              <th>密码更新</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {users.map((user) => (
              <tr key={user.id}>
                <td>{user.username}</td>
                <td>{user.nickname ?? "-"}</td>
                <td>{user.employeeName ?? (user.employeeId ? "员工已删除" : "未关联")}</td>
                <td>{user.roles?.join("、") || "-"}</td>
                <td>
                  <span className={`badge ${user.status === "active" ? "badge-green" : "badge-red"}`}>{statusText(user.status)}</span>
                </td>
                <td>{user.lastLoginAt ?? "-"}</td>
                <td>{user.passwordUpdatedAt ?? "-"}</td>
                <td>
                  <div className="table-actions">
                    <Button variant="secondary" onClick={() => edit(user)}>
                      <Save size={16} />
                      编辑
                    </Button>
                    <Button variant="danger" onClick={() => disableMutation.mutate(user.id)} disabled={disableMutation.isPending || user.status === "inactive"}>
                      <Ban size={16} />
                      停用
                    </Button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {userListing.pagination}
      <div className="toolbar">
        <Button type="button" variant="secondary" onClick={() => refetch()} disabled={isFetching}>
          <RefreshCw size={16} />
          刷新
        </Button>
      </div>
    </Page>
  );
}
