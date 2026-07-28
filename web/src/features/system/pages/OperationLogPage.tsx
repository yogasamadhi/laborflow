import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { DataErrorState, RouteLoadingState } from "../../../components/common/AccessState";
import { Field, TextInput } from "../../../components/common/Field";
import { Page } from "../../../components/common/Page";
import { Pagination } from "../../../components/common/Pagination";
import { Button } from "../../../components/ui/Button";
import { getData, PageResult } from "../../../lib/axios";

type OperationLog = {
  id: string;
  module?: string;
  businessType?: string;
  businessId?: string;
  action?: string;
  operatorId?: string;
  operatorName?: string;
  result?: string;
  summary?: string;
  createdAt?: string;
};

type Filters = {
  keyword: string;
  module: string;
  action: string;
  businessId: string;
  operatorId: string;
  createdFrom: string;
  createdTo: string;
};

const emptyFilters: Filters = {
  keyword: "",
  module: "",
  action: "",
  businessId: "",
  operatorId: "",
  createdFrom: "",
  createdTo: ""
};

export function OperationLogPage() {
  const [draft, setDraft] = useState<Filters>(emptyFilters);
  const [filters, setFilters] = useState<Filters>(emptyFilters);
  const [page, setPage] = useState(1);
  const query = useQuery({
    queryKey: ["system-operation-logs", page, filters],
    queryFn: () => getData<PageResult<OperationLog>>("/system/logs", {
      page,
      pageSize: 50,
      keyword: filters.keyword || undefined,
      module: filters.module || undefined,
      action: filters.action || undefined,
      businessId: filters.businessId || undefined,
      operatorId: filters.operatorId || undefined,
      createdFrom: filters.createdFrom || undefined,
      createdTo: filters.createdTo || undefined
    })
  });
  const total = Number(query.data?.total ?? 0);
  const pageSize = Number(query.data?.pageSize ?? 50);
  const rows = query.data?.records ?? [];

  return (
    <Page title="操作日志" description="按操作者、模块、动作、业务编号和时间定位关键业务证据。">
      <form className="panel toolbar" onSubmit={(event) => { event.preventDefault(); setPage(1); setFilters(draft); }}>
        <Field label="关键词"><TextInput value={draft.keyword} onChange={event => setDraft({ ...draft, keyword: event.target.value })} placeholder="操作者、动作或内容" /></Field>
        <Field label="模块"><TextInput value={draft.module} onChange={event => setDraft({ ...draft, module: event.target.value })} placeholder="salary / project" /></Field>
        <Field label="动作"><TextInput value={draft.action} onChange={event => setDraft({ ...draft, action: event.target.value })} /></Field>
        <Field label="业务编号"><TextInput value={draft.businessId} onChange={event => setDraft({ ...draft, businessId: event.target.value.replace(/\D/g, "") })} /></Field>
        <Field label="操作人 ID"><TextInput value={draft.operatorId} onChange={event => setDraft({ ...draft, operatorId: event.target.value.replace(/\D/g, "") })} /></Field>
        <Field label="开始时间"><TextInput type="datetime-local" value={draft.createdFrom} onChange={event => setDraft({ ...draft, createdFrom: event.target.value })} /></Field>
        <Field label="结束时间"><TextInput type="datetime-local" value={draft.createdTo} onChange={event => setDraft({ ...draft, createdTo: event.target.value })} /></Field>
        <Button type="submit">查询</Button>
        <Button type="button" variant="secondary" onClick={() => { setDraft(emptyFilters); setFilters(emptyFilters); setPage(1); }}>重置</Button>
      </form>
      {query.isPending ? <RouteLoadingState /> : null}
      {query.error ? <DataErrorState message={query.error instanceof Error ? query.error.message : undefined} onRetry={() => void query.refetch()} /> : null}
      {!query.isPending && !query.error ? (
        <>
          <div className="panel table-wrap">
            {rows.length === 0 ? <div className="empty-state">没有符合条件的操作日志。</div> : (
              <table>
                <thead><tr><th>时间</th><th>操作者</th><th>模块</th><th>动作</th><th>业务类型</th><th>业务编号</th><th>结果</th><th>摘要</th></tr></thead>
                <tbody>{rows.map(row => <tr key={row.id}>
                  <td>{row.createdAt?.replace("T", " ").slice(0, 19) ?? "-"}</td>
                  <td>{row.operatorName ?? "系统"}</td>
                  <td>{row.module ?? "-"}</td>
                  <td>{row.action ?? "-"}</td>
                  <td>{row.businessType ?? "-"}</td>
                  <td>{row.businessId ?? "-"}</td>
                  <td>{row.result === "success" ? "成功" : row.result ?? "-"}</td>
                  <td>{row.summary ?? "-"}</td>
                </tr>)}</tbody>
              </table>
            )}
          </div>
          <Pagination page={page} pageSize={pageSize} total={total} onPageChange={setPage} />
        </>
      ) : null}
    </Page>
  );
}
