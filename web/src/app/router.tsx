import { createBrowserRouter, Navigate } from "react-router-dom";
import { AppLayout } from "../components/layout/AppLayout";
import { LoginPage } from "../features/auth/pages/LoginPage";
import { DashboardPage } from "../features/dashboard/pages/DashboardPage";
import { EmployeeDetailPage } from "../features/employee/pages/EmployeeDetailPage";
import { EmployeeFormPage } from "../features/employee/pages/EmployeeFormPage";
import { EmployeeListPage } from "../features/employee/pages/EmployeeListPage";
import { SimpleCrudPage } from "../components/common/SimpleCrudPage";
import { SalaryItemPage } from "../features/salary-item/pages/SalaryItemPage";
import { SalaryProfilePage } from "../features/salary-profile/pages/SalaryProfilePage";
import { SalaryMonthlyCreatePage } from "../features/salary-monthly/pages/SalaryMonthlyCreatePage";
import { SalaryMonthlyDetailPage } from "../features/salary-monthly/pages/SalaryMonthlyDetailPage";
import { SalaryMonthlyListPage } from "../features/salary-monthly/pages/SalaryMonthlyListPage";
import { SalaryMonthlyPaymentPage } from "../features/salary-monthly/pages/SalaryMonthlyPaymentPage";
import { SalaryStatisticsPage } from "../features/statistics/pages/SalaryStatisticsPage";

export const router = createBrowserRouter([
  { path: "/login", element: <LoginPage /> },
  {
    path: "/",
    element: <AppLayout />,
    children: [
      { index: true, element: <Navigate to="/dashboard" replace /> },
      { path: "dashboard", element: <DashboardPage /> },
      { path: "employees", element: <EmployeeListPage /> },
      { path: "employees/create", element: <EmployeeFormPage /> },
      { path: "employees/:id", element: <EmployeeDetailPage /> },
      { path: "employees/:id/edit", element: <EmployeeFormPage /> },
      { path: "departments", element: <SimpleCrudPage title="部门管理" resource="departments" /> },
      { path: "positions", element: <SimpleCrudPage title="岗位管理" resource="positions" /> },
      { path: "salary/items", element: <SalaryItemPage /> },
      { path: "salary/profiles", element: <SalaryProfilePage /> },
      { path: "salary/monthly", element: <SalaryMonthlyListPage /> },
      { path: "salary/monthly/create", element: <SalaryMonthlyCreatePage /> },
      { path: "salary/monthly/:id", element: <SalaryMonthlyDetailPage /> },
      { path: "salary/monthly/:id/edit", element: <SalaryMonthlyDetailPage /> },
      { path: "salary/monthly/:id/payment", element: <SalaryMonthlyPaymentPage /> },
      { path: "statistics/salary", element: <SalaryStatisticsPage /> },
      { path: "system/users", element: <SimpleCrudPage title="用户管理" resource="system/users" readonly /> },
      { path: "system/roles", element: <SimpleCrudPage title="角色管理" resource="system/roles" readonly /> },
      { path: "system/logs", element: <SimpleCrudPage title="操作日志" resource="system/logs" readonly /> }
    ]
  }
]);

