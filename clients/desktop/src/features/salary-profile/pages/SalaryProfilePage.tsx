import { useMutation, useQueries, useQuery, useQueryClient } from "@tanstack/react-query";
import { Edit, Plus, RotateCcw, Save, Trash2 } from "lucide-react";
import { FormEvent, useMemo, useState } from "react";
import { Field, SelectInput, TextInput } from "../../../components/common/Field";
import { Page } from "../../../components/common/Page";
import { usePagedQuery } from "../../../components/common/usePagedQuery";
import { Button } from "../../../components/ui/Button";
import { getAllPages, getData, postData, putData } from "../../../lib/axios";
import { money, statusText } from "../../../lib/utils";
import type { Employee } from "../../employee/types";

type Profile = {
  id: string;
  employeeId: string;
  effectiveDate?: string;
  status: string;
  remark?: string;
};

type SalaryItem = {
  id: string;
  name: string;
  type: "earning" | "deduction";
  status: string;
};

type ProfileItem = {
  id?: string;
  salaryItemId?: string;
  salaryItemName: string;
  salaryItemType: "earning" | "deduction";
  amount: number;
  remark?: string;
};

type ProfileDetail = {
  profile: Profile;
  items: ProfileItem[];
};

type ProfileForm = {
  id?: string;
  employeeId: string;
  effectiveDate: string;
  status: "active" | "inactive";
  remark: string;
  items: ProfileItem[];
};

const emptyForm: ProfileForm = {
  employeeId: "",
  effectiveDate: new Date().toISOString().slice(0, 10),
  status: "active",
  remark: "",
  items: []
};

function newItem(item?: SalaryItem): ProfileItem {
  return {
    salaryItemId: item?.id,
    salaryItemName: item?.name ?? "",
    salaryItemType: item?.type ?? "earning",
    amount: 0,
    remark: ""
  };
}

function calcTotals(items: ProfileItem[]) {
  return items.reduce(
    (total, item) => {
      const amount = Number(item.amount ?? 0);
      if (item.salaryItemType === "deduction") {
        total.deduction += amount;
      } else {
        total.earning += amount;
      }
      total.net = total.earning - total.deduction;
      return total;
    },
    { earning: 0, deduction: 0, net: 0 }
  );
}

export function SalaryProfilePage() {
  const queryClient = useQueryClient();
  const [form, setForm] = useState<ProfileForm>(emptyForm);
  const [employeeFilter, setEmployeeFilter] = useState("");
  const [error, setError] = useState("");

  const profileListing = usePagedQuery<Profile>(["salary-profiles"], "/salary/profiles");
  const profilesData = profileListing.data;
  const { data: employeesData } = useQuery({
    queryKey: ["employees", "salary-profile-options"],
    queryFn: () => getAllPages<Employee>("/reference/employees", { status: "active" })
  });
  const { data: salaryItemsData } = useQuery({
    queryKey: ["salary-items", "salary-profile-options"],
    queryFn: () => getAllPages<SalaryItem>("/salary/items")
  });

  const profiles = profilesData?.records ?? [];
  const employees = employeesData ?? [];
  const salaryItems = (salaryItemsData ?? []).filter((item) => item.status !== "inactive");
  const employeeMap = useMemo(() => new Map(employees.map((employee) => [employee.id, employee])), [employees]);
  const profileDetailQueries = useQueries({
    queries: profiles.map((profile) => ({
      queryKey: ["salary-profile-detail", profile.id],
      queryFn: () => getData<ProfileDetail>(`/salary/profiles/${profile.id}`)
    }))
  });
  const detailMap = useMemo(() => {
    const map = new Map<string, ProfileDetail>();
    profileDetailQueries.forEach((query) => {
      if (query.data?.profile?.id) {
        map.set(query.data.profile.id, query.data);
      }
    });
    return map;
  }, [profileDetailQueries]);
  const visibleProfiles = useMemo(() => {
    const keyword = employeeFilter.trim();
    if (!keyword) return profiles;
    return profiles.filter((profile) => {
      const employee = employeeMap.get(profile.employeeId);
      return employee?.name?.includes(keyword);
    });
  }, [employeeFilter, employeeMap, profiles]);
  const selectedEmployeeIds = new Set(profiles.filter((profile) => profile.status === "active").map((profile) => profile.employeeId));
  const totals = calcTotals(form.items);

  const save = useMutation({
    mutationFn: () => {
      if (!form.employeeId) throw new Error("请选择员工");
      if (form.items.length === 0) throw new Error("请至少添加一个工资项");
      const payload = {
        employeeId: form.employeeId,
        effectiveDate: form.effectiveDate || undefined,
        status: form.status,
        remark: form.remark,
        items: form.items.map((item) => ({
          salaryItemId: item.salaryItemId,
          salaryItemName: item.salaryItemName.trim(),
          salaryItemType: item.salaryItemType,
          amount: Number(item.amount ?? 0),
          remark: item.remark
        }))
      };
      return form.id ? putData<Profile>(`/salary/profiles/${form.id}`, payload) : postData<Profile>("/salary/profiles", payload);
    },
    onSuccess: async () => {
      setError("");
      setForm(emptyForm);
      await queryClient.invalidateQueries({ queryKey: ["salary-profiles"] });
      await queryClient.invalidateQueries({ queryKey: ["salary-profile-detail"] });
    },
    onError: (reason) => setError(reason instanceof Error ? reason.message : "保存失败")
  });

  const statusMutation = useMutation({
    mutationFn: ({ profile, status }: { profile: Profile; status: "active" | "inactive" }) => {
      const detail = detailMap.get(profile.id);
      return putData<Profile>(`/salary/profiles/${profile.id}`, {
        employeeId: profile.employeeId,
        effectiveDate: profile.effectiveDate,
        status,
        remark: profile.remark,
        items: (detail?.items ?? []).map((item) => ({
          salaryItemId: item.salaryItemId,
          salaryItemName: item.salaryItemName,
          salaryItemType: item.salaryItemType,
          amount: item.amount,
          remark: item.remark
        }))
      });
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["salary-profiles"] });
      await queryClient.invalidateQueries({ queryKey: ["salary-profile-detail"] });
    }
  });

  function submit(event: FormEvent) {
    event.preventDefault();
    save.mutate();
  }

  function reset() {
    setError("");
    setForm(emptyForm);
  }

  function edit(profile: Profile) {
    const detail = detailMap.get(profile.id);
    setError("");
    setForm({
      id: profile.id,
      employeeId: String(profile.employeeId),
      effectiveDate: profile.effectiveDate ?? "",
      status: profile.status === "inactive" ? "inactive" : "active",
      remark: profile.remark ?? "",
      items: (detail?.items ?? []).map((item) => ({
        salaryItemId: item.salaryItemId,
        salaryItemName: item.salaryItemName,
        salaryItemType: item.salaryItemType,
        amount: Number(item.amount ?? 0),
        remark: item.remark ?? ""
      }))
    });
  }

  function addItem() {
    setForm((current) => ({ ...current, items: [...current.items, newItem(salaryItems[0])] }));
  }

  function updateItem(index: number, patch: Partial<ProfileItem>) {
    setForm((current) => ({
      ...current,
      items: current.items.map((item, itemIndex) => (itemIndex === index ? { ...item, ...patch } : item))
    }));
  }

  function selectSalaryItem(index: number, itemId: string) {
    const item = salaryItems.find((option) => String(option.id) === itemId);
    updateItem(index, item ? newItem(item) : { salaryItemId: undefined, salaryItemName: "", salaryItemType: "earning" });
  }

  function removeItem(index: number) {
    setForm((current) => ({ ...current, items: current.items.filter((_, itemIndex) => itemIndex !== index) }));
  }

  return (
    <Page title="员工工资档案">
      <div className="grid grid-2 profile-layout">
        <form className="panel grid" onSubmit={submit}>
          <div className="section-heading">
            <div>
              <h3>{form.id ? "编辑工资档案" : "新增工资档案"}</h3>
              <div className="muted">维护员工默认工资结构，生成月度工资表时会读取启用档案。</div>
            </div>
            <Button type="button" variant="secondary" onClick={reset}>
              <RotateCcw size={16} />
              重置
            </Button>
          </div>
          {error && <div className="form-error">{error}</div>}
          <div className="grid grid-2">
            <Field label="员工">
              <SelectInput value={form.employeeId} onChange={(event) => setForm({ ...form, employeeId: event.target.value })} required>
                <option value="">请选择员工</option>
                {employees.map((employee) => {
                  const hasActiveProfile = selectedEmployeeIds.has(employee.id) && employee.id !== form.employeeId;
                  return (
                    <option key={employee.id} value={employee.id}>
                      {employee.name}{hasActiveProfile ? " / 已有启用档案" : ""}
                    </option>
                  );
                })}
              </SelectInput>
            </Field>
            <Field label="生效日期">
              <TextInput type="date" value={form.effectiveDate} onChange={(event) => setForm({ ...form, effectiveDate: event.target.value })} />
            </Field>
            <Field label="状态">
              <SelectInput value={form.status} onChange={(event) => setForm({ ...form, status: event.target.value as ProfileForm["status"] })}>
                <option value="active">启用</option>
                <option value="inactive">停用</option>
              </SelectInput>
            </Field>
            <Field label="备注">
              <TextInput value={form.remark} onChange={(event) => setForm({ ...form, remark: event.target.value })} />
            </Field>
          </div>

          <div className="section-heading">
            <div>
              <h3>工资项明细</h3>
              <div className="muted">应发项累加到应发金额，扣减项累加到扣款金额。</div>
            </div>
            <Button type="button" variant="secondary" onClick={addItem}>
              <Plus size={16} />
              添加工资项
            </Button>
          </div>

          {form.items.length === 0 ? (
            <div className="empty-state">还没有工资项，点击“添加工资项”开始配置。</div>
          ) : (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>工资项</th>
                    <th>类型</th>
                    <th>金额</th>
                    <th>备注</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  {form.items.map((item, index) => (
                    <tr key={`${item.salaryItemId ?? "custom"}-${index}`}>
                      <td>
                        <SelectInput value={item.salaryItemId ?? ""} onChange={(event) => selectSalaryItem(index, event.target.value)}>
                          <option value="">自定义项目</option>
                          {salaryItems.map((option) => <option key={option.id} value={option.id}>{option.name}</option>)}
                        </SelectInput>
                        {!item.salaryItemId && (
                          <TextInput
                            className="inline-input"
                            placeholder="自定义工资项名称"
                            value={item.salaryItemName}
                            onChange={(event) => updateItem(index, { salaryItemName: event.target.value })}
                            required
                          />
                        )}
                      </td>
                      <td>
                        <SelectInput value={item.salaryItemType} onChange={(event) => updateItem(index, { salaryItemType: event.target.value as ProfileItem["salaryItemType"] })}>
                          <option value="earning">应发项</option>
                          <option value="deduction">扣减项</option>
                        </SelectInput>
                      </td>
                      <td>
                        <TextInput type="number" step="0.01" value={item.amount} onChange={(event) => updateItem(index, { amount: Number(event.target.value) })} />
                      </td>
                      <td>
                        <TextInput value={item.remark ?? ""} onChange={(event) => updateItem(index, { remark: event.target.value })} />
                      </td>
                      <td>
                        <Button type="button" variant="danger" onClick={() => removeItem(index)}>
                          <Trash2 size={16} />
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          <div className="summary-strip">
            <span>应发：{money(totals.earning)}</span>
            <span>扣减：{money(totals.deduction)}</span>
            <strong>档案净额：{money(totals.net)}</strong>
          </div>
          <div className="toolbar">
            <Button type="submit" disabled={save.isPending}>
              <Save size={16} />
              {save.isPending ? "保存中" : form.id ? "保存修改" : "保存档案"}
            </Button>
          </div>
        </form>

        <div className="panel grid">
          <div className="section-heading">
            <div>
              <h3>档案列表</h3>
              <div className="muted">可按员工姓名过滤，点击编辑会载入完整明细。</div>
            </div>
            <Field label="员工筛选">
              <TextInput value={employeeFilter} onChange={(event) => setEmployeeFilter(event.target.value)} placeholder="员工姓名" />
            </Field>
          </div>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>员工</th>
                  <th>生效日期</th>
                  <th>状态</th>
                  <th>应发</th>
                  <th>扣减</th>
                  <th>净额</th>
                  <th>工资项</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                {visibleProfiles.map((profile) => {
                  const detail = detailMap.get(profile.id);
                  const rowTotals = calcTotals(detail?.items ?? []);
                  const employee = employeeMap.get(profile.employeeId);
                  return (
                    <tr key={profile.id}>
                      <td>{employee?.name ?? "已删除员工"}</td>
                      <td>{profile.effectiveDate ?? "-"}</td>
                      <td><span className={profile.status === "active" ? "badge badge-green" : "badge badge-yellow"}>{profile.status === "active" ? "启用" : statusText(profile.status)}</span></td>
                      <td>{money(rowTotals.earning)}</td>
                      <td>{money(rowTotals.deduction)}</td>
                      <td>{money(rowTotals.net)}</td>
                      <td>{detail?.items?.length ?? "-"}</td>
                      <td>
                        <Button type="button" variant="secondary" onClick={() => edit(profile)} disabled={!detail}>
                          <Edit size={16} />
                          编辑
                        </Button>{" "}
                        <Button
                          type="button"
                          variant={profile.status === "active" ? "danger" : "secondary"}
                          onClick={() => statusMutation.mutate({ profile, status: profile.status === "active" ? "inactive" : "active" })}
                          disabled={!detail || statusMutation.isPending}
                        >
                          {profile.status === "active" ? "停用" : "启用"}
                        </Button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
          {profileListing.pagination}
        </div>
      </div>
    </Page>
  );
}
