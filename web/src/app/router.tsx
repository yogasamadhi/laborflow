import { createBrowserRouter, Navigate } from "react-router-dom";
import { AppLayout } from "../components/layout/AppLayout";
import { NotFoundPage } from "../components/common/AccessState";
import { LoginPage } from "../features/auth/pages/LoginPage";
import { DashboardPage } from "../features/dashboard/pages/DashboardPage";
import { EmployeeDetailPage } from "../features/employee/pages/EmployeeDetailPage";
import { EmployeeFormPage } from "../features/employee/pages/EmployeeFormPage";
import { EmployeeListPage } from "../features/employee/pages/EmployeeListPage";
import { EmployeeSalaryHistoryPage } from "../features/employee/pages/EmployeeSalaryHistoryPage";
import { SimpleCrudPage } from "../components/common/SimpleCrudPage";
import { DepartmentPage } from "../features/department/pages/DepartmentPage";
import { PositionPage } from "../features/position/pages/PositionPage";
import { SalaryItemPage } from "../features/salary-item/pages/SalaryItemPage";
import { SalaryProfilePage } from "../features/salary-profile/pages/SalaryProfilePage";
import { SalaryMonthlyCreatePage } from "../features/salary-monthly/pages/SalaryMonthlyCreatePage";
import { SalaryMonthlyDetailPage } from "../features/salary-monthly/pages/SalaryMonthlyDetailPage";
import { SalaryMonthlyExportPage } from "../features/salary-monthly/pages/SalaryMonthlyExportPage";
import { SalaryMonthlyListPage } from "../features/salary-monthly/pages/SalaryMonthlyListPage";
import { SalaryMonthlyPaymentPage } from "../features/salary-monthly/pages/SalaryMonthlyPaymentPage";
import { SalaryMonthlyPayslipPage } from "../features/salary-monthly/pages/SalaryMonthlyPayslipPage";
import { SalaryAnomaliesPage } from "../features/statistics/pages/SalaryAnomaliesPage";
import { SalaryStatisticsPage } from "../features/statistics/pages/SalaryStatisticsPage";
import {
  AttendancePage,
  BonusAllocationPage,
  BonusPoolPage,
  CompensationDashboardPage,
  CompensationSalaryPage,
  PerformanceIndicatorPage,
  PerformanceScorePage,
  SalaryDeductionPage
} from "../features/compensation/pages/CompensationPages";
import {
  DailyWorkReportPage,
  DocumentArchivePage,
  MachineryPage,
  MaterialRecordPage,
  ProjectCostPage,
  ProjectContractItemPage,
  ProjectDashboardPage,
  ProjectLedgerPage,
  ProjectPaymentPage,
  ProjectRiskPage,
  ProjectSectionPage,
  TeamPage,
  TeamSettlementPage,
  TeamUnitPricePage,
  VariationOrderPage,
  WorkConfirmationPage,
  WorkProcessPage
} from "../features/project/pages/ProjectPages";
import { WechatBindingPage } from "../features/system/pages/WechatBindingPage";
import { SystemUserPage } from "../features/system/pages/SystemUserPage";

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
      { path: "employees/:id/salary-history", element: <EmployeeSalaryHistoryPage /> },
      { path: "departments", element: <DepartmentPage /> },
      { path: "positions", element: <PositionPage /> },
      { path: "salary/items", element: <SalaryItemPage /> },
      { path: "salary/profiles", element: <SalaryProfilePage /> },
      { path: "salary/monthly", element: <SalaryMonthlyListPage /> },
      { path: "salary/monthly/create", element: <SalaryMonthlyCreatePage /> },
      { path: "salary/monthly/:id", element: <SalaryMonthlyDetailPage /> },
      { path: "salary/monthly/:id/edit", element: <SalaryMonthlyDetailPage /> },
      { path: "salary/monthly/:id/export", element: <SalaryMonthlyExportPage /> },
      { path: "salary/monthly/:id/payslip", element: <SalaryMonthlyPayslipPage /> },
      { path: "salary/monthly/:id/payment", element: <SalaryMonthlyPaymentPage /> },
      { path: "statistics/salary", element: <SalaryStatisticsPage /> },
      { path: "statistics/salary/anomalies", element: <SalaryAnomaliesPage /> },
      { path: "projects", element: <ProjectLedgerPage /> },
      { path: "projects/sections", element: <ProjectSectionPage /> },
      { path: "projects/processes", element: <WorkProcessPage /> },
      { path: "projects/contract-items", element: <ProjectContractItemPage /> },
      { path: "projects/daily-reports", element: <DailyWorkReportPage /> },
      { path: "projects/work-confirmation", element: <WorkConfirmationPage /> },
      { path: "projects/teams", element: <TeamPage /> },
      { path: "projects/team-prices", element: <TeamUnitPricePage /> },
      { path: "projects/team-settlements", element: <TeamSettlementPage /> },
      { path: "projects/variations", element: <VariationOrderPage /> },
      { path: "projects/payments", element: <ProjectPaymentPage /> },
      { path: "projects/costs", element: <ProjectCostPage /> },
      { path: "projects/machinery", element: <MachineryPage /> },
      { path: "projects/materials", element: <MaterialRecordPage /> },
      { path: "projects/documents", element: <DocumentArchivePage /> },
      { path: "projects/risks", element: <ProjectRiskPage /> },
      { path: "projects/dashboard", element: <ProjectDashboardPage /> },
      { path: "compensation/attendance", element: <AttendancePage /> },
      { path: "compensation/deductions", element: <SalaryDeductionPage /> },
      { path: "compensation/indicators", element: <PerformanceIndicatorPage /> },
      { path: "compensation/scores", element: <PerformanceScorePage /> },
      { path: "compensation/bonus-pools", element: <BonusPoolPage /> },
      { path: "compensation/bonus-allocations", element: <BonusAllocationPage /> },
      { path: "compensation/salary", element: <CompensationSalaryPage /> },
      { path: "compensation/dashboard", element: <CompensationDashboardPage /> },
      { path: "system/users", element: <SystemUserPage /> },
      { path: "system/wechat-bindings", element: <WechatBindingPage /> },
      { path: "system/roles", element: <SimpleCrudPage title="角色管理" resource="system/roles" readonly /> },
      { path: "system/logs", element: <SimpleCrudPage title="操作日志" resource="system/logs" readonly /> },
      { path: "*", element: <NotFoundPage /> }
    ]
  }
]);
