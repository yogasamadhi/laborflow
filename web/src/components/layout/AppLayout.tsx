import { ChevronDown, ChevronRight, LogOut, X } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import { canAccessRoles, canAccessRoute, roleGroups } from "../../lib/access";
import { getData, REQUEST_ERROR_EVENT } from "../../lib/axios";
import { clearToken, getLoginUser, getToken, LoginUser, setLoginUser } from "../../lib/auth";
import type { RequestErrorDetail } from "../../lib/axios";
import { AccessDeniedState, AuthLoadingState } from "../common/AccessState";
import { Button } from "../ui/Button";

const {
  admin: adminRoles,
  org: orgRoles,
  salary: salaryRoles,
  project: projectRoles,
  projectFinance: projectFinanceRoles,
  compensation: compensationRoles
} = roleGroups;

const groups = [
  {
    title: "工作台",
    links: [
      { to: "/dashboard", label: "仪表盘", end: true },
      { to: "/employees", label: "员工信息", end: true, roles: orgRoles },
      { to: "/departments", label: "部门管理", roles: orgRoles },
      { to: "/positions", label: "岗位管理", roles: orgRoles }
    ]
  },
  {
    title: "工资管理",
    links: [
      { to: "/salary/items", label: "工资项", roles: salaryRoles },
      { to: "/salary/profiles", label: "工资档案", roles: salaryRoles },
      { to: "/salary/monthly", label: "月度工资", end: true, roles: salaryRoles },
      { to: "/statistics/salary", label: "工资统计", roles: salaryRoles },
      { to: "/statistics/salary/anomalies", label: "异常提醒", roles: salaryRoles }
    ]
  },
  {
    title: "项目经营",
    links: [
      { to: "/projects", label: "项目台账", end: true, roles: projectRoles },
      { to: "/projects/sections", label: "施工段管理", roles: projectRoles },
      { to: "/projects/processes", label: "工序库", roles: projectRoles },
      { to: "/projects/contract-items", label: "合同清单", roles: projectRoles },
      { to: "/projects/daily-reports", label: "工程量日报", roles: projectRoles },
      { to: "/projects/work-confirmation", label: "工程量确认", roles: projectRoles },
      { to: "/projects/teams", label: "班组管理", roles: projectRoles },
      { to: "/projects/team-prices", label: "班组单价", roles: projectRoles },
      { to: "/projects/team-settlements", label: "班组结算", roles: projectFinanceRoles },
      { to: "/projects/variations", label: "签证变更", roles: projectRoles },
      { to: "/projects/payments", label: "项目回款", roles: projectFinanceRoles },
      { to: "/projects/costs", label: "项目成本", roles: projectFinanceRoles },
      { to: "/projects/machinery", label: "机械台班", roles: projectRoles },
      { to: "/projects/materials", label: "材料辅材", roles: projectRoles },
      { to: "/projects/documents", label: "资料归档", roles: projectRoles },
      { to: "/projects/risks", label: "风险预警", roles: projectFinanceRoles },
      { to: "/projects/dashboard", label: "项目经营看板", roles: projectFinanceRoles }
    ]
  },
  {
    title: "薪酬与绩效",
    links: [
      { to: "/compensation/attendance", label: "考勤管理", roles: compensationRoles },
      { to: "/compensation/deductions", label: "工资扣款", roles: compensationRoles },
      { to: "/compensation/indicators", label: "绩效指标", roles: compensationRoles },
      { to: "/compensation/scores", label: "绩效评分", roles: compensationRoles },
      { to: "/compensation/bonus-pools", label: "项目奖金池", roles: compensationRoles },
      { to: "/compensation/bonus-allocations", label: "奖金分配", roles: compensationRoles },
      { to: "/compensation/salary", label: "工资表", roles: compensationRoles },
      { to: "/compensation/dashboard", label: "薪酬绩效看板", roles: compensationRoles }
    ]
  },
  {
    title: "系统",
    links: [
      { to: "/system/users", label: "用户", roles: adminRoles },
      { to: "/system/wechat-bindings", label: "微信绑定", roles: adminRoles },
      { to: "/system/roles", label: "角色", roles: adminRoles },
      { to: "/system/logs", label: "操作日志", roles: adminRoles }
    ]
  }
];

export function AppLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const [user, setUser] = useState<LoginUser | null>(() => (getToken() ? getLoginUser() : null));
  const [isCheckingAuth, setIsCheckingAuth] = useState(() => Boolean(getToken()) && !getLoginUser());
  const [accessError, setAccessError] = useState<string | null>(null);
  const roles = user?.roles ?? [];
  const visibleGroups = useMemo(() => {
    return groups
      .map((group) => ({
        ...group,
        links: group.links.filter((link) => canAccessRoles(roles, link.roles))
      }))
      .filter((group) => group.links.length > 0);
  }, [roles]);
  const activeGroup = useMemo(() => {
    return visibleGroups.find((group) => group.links.some((link) => {
      if (link.end) return location.pathname === link.to;
      return location.pathname === link.to || location.pathname.startsWith(`${link.to}/`);
    }))?.title;
  }, [location.pathname, visibleGroups]);
  const [openGroups, setOpenGroups] = useState<Record<string, boolean>>(() => {
    const initial = Object.fromEntries(groups.map((group, index) => [group.title, index === 0]));
    return activeGroup ? { ...initial, [activeGroup]: true } : initial;
  });

  function loginPath(reason: "login-required" | "session-expired") {
    const next = `${location.pathname}${location.search}${location.hash}`;
    return `/login?reason=${reason}&next=${encodeURIComponent(next)}`;
  }

  useEffect(() => {
    function handleRequestError(event: Event) {
      const detail = (event as CustomEvent<RequestErrorDetail>).detail;
      if (detail?.status === 403) setAccessError(detail.message);
    }
    window.addEventListener(REQUEST_ERROR_EVENT, handleRequestError);
    return () => window.removeEventListener(REQUEST_ERROR_EVENT, handleRequestError);
  }, []);

  useEffect(() => {
    if (!getToken()) {
      clearToken();
      setUser(null);
      setIsCheckingAuth(false);
      navigate(loginPath("login-required"), { replace: true });
      return;
    }
    if (user) {
      setIsCheckingAuth(false);
      return;
    }
    setIsCheckingAuth(true);
    getData<LoginUser>("/auth/me")
      .then((currentUser) => {
        setLoginUser(currentUser);
        setUser(currentUser);
      })
      .catch(() => {
        clearToken();
        navigate(loginPath("session-expired"), { replace: true });
      })
      .finally(() => setIsCheckingAuth(false));
  }, [location.hash, location.pathname, location.search, navigate, user]);

  useEffect(() => {
    if (activeGroup) {
      setOpenGroups((current) => ({ ...current, [activeGroup]: true }));
    }
  }, [activeGroup]);

  function logout() {
    clearToken();
    setUser(null);
    navigate("/login?reason=signed-out", { replace: true });
  }

  if (isCheckingAuth || !user) return <AuthLoadingState />;

  const routeAllowed = canAccessRoute(location.pathname, roles);

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <h1 className="brand">LaborFlow</h1>
        {visibleGroups.map((group) => (
          <div className="nav-group" key={group.title}>
            <button
              className="nav-title-button"
              type="button"
              onClick={() => setOpenGroups((current) => ({ ...current, [group.title]: !current[group.title] }))}
              aria-expanded={openGroups[group.title]}
            >
              <span>{group.title}</span>
              {openGroups[group.title] ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
            </button>
            {openGroups[group.title] && (
              <div className="nav-links">
                {group.links.map((link) => (
                  <NavLink className="nav-link" end={link.end} key={link.to} to={link.to}>
                    {link.label}
                  </NavLink>
                ))}
              </div>
            )}
          </div>
        ))}
      </aside>
      <main className="main">
        <div className="topbar">
          <div className="muted">建筑劳务经营与薪酬管理平台</div>
          <Button variant="secondary" onClick={logout}>
            <LogOut size={16} />
            退出
          </Button>
        </div>
        {accessError ? (
          <div className="access-notice form-error" role="alert">
            <span>{accessError}</span>
            <button className="icon-button" type="button" onClick={() => setAccessError(null)} aria-label="关闭权限提示">
              <X size={16} />
            </button>
          </div>
        ) : null}
        {routeAllowed ? <Outlet /> : <AccessDeniedState onBack={() => navigate("/dashboard", { replace: true })} />}
      </main>
    </div>
  );
}
