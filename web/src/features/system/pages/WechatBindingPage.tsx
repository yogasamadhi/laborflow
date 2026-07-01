import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Ban, Link2, RefreshCw, Unlink } from "lucide-react";
import { FormEvent, useState } from "react";
import { getData, PageResult, postData } from "../../../lib/axios";
import { statusText } from "../../../lib/utils";
import { Button } from "../../../components/ui/Button";
import { Field, TextInput } from "../../../components/common/Field";
import { Page } from "../../../components/common/Page";

type WechatBinding = {
  id: string;
  openid: string;
  unionid?: string | null;
  userId?: string | null;
  employeeId?: string | null;
  nickname?: string | null;
  status: string;
  lastSessionAt?: string | null;
  lastLoginAt?: string | null;
};

export function WechatBindingPage() {
  const queryClient = useQueryClient();
  const [selected, setSelected] = useState<WechatBinding | null>(null);
  const [userId, setUserId] = useState("");
  const [employeeId, setEmployeeId] = useState("");
  const [nickname, setNickname] = useState("");
  const { data, isFetching, refetch } = useQuery({
    queryKey: ["system/wechat-bindings"],
    queryFn: () => getData<PageResult<WechatBinding>>("/system/wechat-bindings", { page: 1, pageSize: 100 })
  });
  const rows = data?.records ?? [];
  const invalidate = () => queryClient.invalidateQueries({ queryKey: ["system/wechat-bindings"] });

  const bindMutation = useMutation({
    mutationFn: () => postData(`/system/wechat-bindings/${selected?.id}/bind`, {
      userId: Number(userId),
      employeeId: employeeId ? Number(employeeId) : undefined,
      nickname: nickname || undefined
    }),
    onSuccess: () => {
      setSelected(null);
      setUserId("");
      setEmployeeId("");
      setNickname("");
      invalidate();
    }
  });
  const unbindMutation = useMutation({
    mutationFn: (id: string) => postData(`/system/wechat-bindings/${id}/unbind`),
    onSuccess: invalidate
  });
  const disableMutation = useMutation({
    mutationFn: (id: string) => postData(`/system/wechat-bindings/${id}/disable`),
    onSuccess: invalidate
  });

  function edit(row: WechatBinding) {
    setSelected(row);
    setUserId(row.userId ?? "");
    setEmployeeId(row.employeeId ?? "");
    setNickname(row.nickname ?? "");
  }

  function submit(event: FormEvent) {
    event.preventDefault();
    if (!selected) return;
    bindMutation.mutate();
  }

  return (
    <Page title="微信绑定">
      <form className="panel toolbar" onSubmit={submit}>
        <Field label="绑定记录">
          <TextInput value={selected ? selected.openid : ""} readOnly placeholder="从列表选择 openid" />
        </Field>
        <Field label="用户 ID">
          <TextInput value={userId} onChange={(event) => setUserId(event.target.value)} required disabled={!selected} />
        </Field>
        <Field label="员工 ID">
          <TextInput value={employeeId} onChange={(event) => setEmployeeId(event.target.value)} disabled={!selected} />
        </Field>
        <Field label="微信昵称">
          <TextInput value={nickname} onChange={(event) => setNickname(event.target.value)} disabled={!selected} />
        </Field>
        <Button type="submit" disabled={!selected || bindMutation.isPending}>
          <Link2 size={16} />
          绑定
        </Button>
        <Button type="button" variant="secondary" onClick={() => refetch()} disabled={isFetching}>
          <RefreshCw size={16} />
          刷新
        </Button>
      </form>
      <div className="panel table-wrap">
        <table>
          <thead>
            <tr>
              <th>OpenID</th>
              <th>UnionID</th>
              <th>用户</th>
              <th>员工</th>
              <th>昵称</th>
              <th>状态</th>
              <th>最近会话</th>
              <th>最近登录</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr key={row.id}>
                <td>{row.openid}</td>
                <td>{row.unionid ?? "-"}</td>
                <td>{row.userId ?? "-"}</td>
                <td>{row.employeeId ?? "-"}</td>
                <td>{row.nickname ?? "-"}</td>
                <td>
                  <span className={`badge ${row.status === "active" ? "badge-green" : row.status === "inactive" ? "badge-red" : "badge-yellow"}`}>
                    {statusText(row.status)}
                  </span>
                </td>
                <td>{row.lastSessionAt ?? "-"}</td>
                <td>{row.lastLoginAt ?? "-"}</td>
                <td>
                  <div className="table-actions">
                    <Button variant="secondary" onClick={() => edit(row)}>
                      <Link2 size={16} />
                      换绑
                    </Button>
                    <Button variant="secondary" onClick={() => unbindMutation.mutate(row.id)} disabled={unbindMutation.isPending}>
                      <Unlink size={16} />
                      解绑
                    </Button>
                    <Button variant="danger" onClick={() => disableMutation.mutate(row.id)} disabled={disableMutation.isPending}>
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
    </Page>
  );
}
