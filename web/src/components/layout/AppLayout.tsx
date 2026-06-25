import { LogOut } from "lucide-react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { clearToken } from "../../lib/auth";
import { Button } from "../ui/Button";

const groups = [
  {
    title: "工作台",
    links: [
      { to: "/dashboard", label: "仪表盘" },
      { to: "/employees", label: "员工信息" },
      { to: "/departments", label: "部门管理" },
      { to: "/positions", label: "岗位管理" }
    ]
  },
  {
    title: "工资管理",
    links: [
      { to: "/salary/items", label: "工资项" },
      { to: "/salary/profiles", label: "工资档案" },
      { to: "/salary/monthly", label: "月度工资" },
      { to: "/statistics/salary", label: "工资统计" }
    ]
  },
  {
    title: "系统",
    links: [
      { to: "/system/users", label: "用户" },
      { to: "/system/roles", label: "角色" },
      { to: "/system/logs", label: "操作日志" }
    ]
  }
];

export function AppLayout() {
  const navigate = useNavigate();

  function logout() {
    clearToken();
    navigate("/login");
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <h1 className="brand">LaborFlow</h1>
        {groups.map((group) => (
          <div className="nav-group" key={group.title}>
            <div className="nav-title">{group.title}</div>
            {group.links.map((link) => (
              <NavLink className="nav-link" key={link.to} to={link.to}>
                {link.label}
              </NavLink>
            ))}
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
        <Outlet />
      </main>
    </div>
  );
}

