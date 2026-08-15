import { lazy, Suspense, type ReactNode } from "react";
import { createBrowserRouter, createHashRouter, Navigate } from "react-router-dom";
import { AppLayout } from "../components/layout/AppLayout";
import { NotFoundPage, RouteLoadingState } from "../components/common/AccessState";
import { isElectronRuntime } from "../lib/desktop";

const LoginPage = lazy(() => import("../features/auth/pages/LoginPage").then(({ LoginPage: page }) => ({ default: page })));
const DashboardPage = lazy(() => import("../features/dashboard/pages/DashboardPage").then(({ DashboardPage: page }) => ({ default: page })));
const EmployeeDetailPage = lazy(() => import("../features/employee/pages/EmployeeDetailPage").then(({ EmployeeDetailPage: page }) => ({ default: page })));
const EmployeeFormPage = lazy(() => import("../features/employee/pages/EmployeeFormPage").then(({ EmployeeFormPage: page }) => ({ default: page })));
const EmployeeListPage = lazy(() => import("../features/employee/pages/EmployeeListPage").then(({ EmployeeListPage: page }) => ({ default: page })));
const EmployeeSalaryHistoryPage = lazy(() => import("../features/employee/pages/EmployeeSalaryHistoryPage").then(({ EmployeeSalaryHistoryPage: page }) => ({ default: page })));
const SimpleCrudPage = lazy(() => import("../components/common/SimpleCrudPage").then(({ SimpleCrudPage: page }) => ({ default: page })));
const DepartmentPage = lazy(() => import("../features/department/pages/DepartmentPage").then(({ DepartmentPage: page }) => ({ default: page })));
const PositionPage = lazy(() => import("../features/position/pages/PositionPage").then(({ PositionPage: page }) => ({ default: page })));
const SalaryItemPage = lazy(() => import("../features/salary-item/pages/SalaryItemPage").then(({ SalaryItemPage: page }) => ({ default: page })));
const SalaryProfilePage = lazy(() => import("../features/salary-profile/pages/SalaryProfilePage").then(({ SalaryProfilePage: page }) => ({ default: page })));
const SalaryMonthlyCreatePage = lazy(() => import("../features/salary-monthly/pages/SalaryMonthlyCreatePage").then(({ SalaryMonthlyCreatePage: page }) => ({ default: page })));
const SalaryMonthlyDetailPage = lazy(() => import("../features/salary-monthly/pages/SalaryMonthlyDetailPage").then(({ SalaryMonthlyDetailPage: page }) => ({ default: page })));
const SalaryMonthlyExportPage = lazy(() => import("../features/salary-monthly/pages/SalaryMonthlyExportPage").then(({ SalaryMonthlyExportPage: page }) => ({ default: page })));
const SalaryMonthlyListPage = lazy(() => import("../features/salary-monthly/pages/SalaryMonthlyListPage").then(({ SalaryMonthlyListPage: page }) => ({ default: page })));
const SalaryMonthlyPaymentPage = lazy(() => import("../features/salary-monthly/pages/SalaryMonthlyPaymentPage").then(({ SalaryMonthlyPaymentPage: page }) => ({ default: page })));
const SalaryMonthlyPayslipPage = lazy(() => import("../features/salary-monthly/pages/SalaryMonthlyPayslipPage").then(({ SalaryMonthlyPayslipPage: page }) => ({ default: page })));
const SalaryAnomaliesPage = lazy(() => import("../features/statistics/pages/SalaryAnomaliesPage").then(({ SalaryAnomaliesPage: page }) => ({ default: page })));
const SalaryStatisticsPage = lazy(() => import("../features/statistics/pages/SalaryStatisticsPage").then(({ SalaryStatisticsPage: page }) => ({ default: page })));
const AttendancePage = lazy(() => import("../features/compensation/pages/CompensationPages").then(({ AttendancePage: page }) => ({ default: page })));
const BonusAllocationPage = lazy(() => import("../features/compensation/pages/CompensationPages").then(({ BonusAllocationPage: page }) => ({ default: page })));
const BonusPoolPage = lazy(() => import("../features/compensation/pages/CompensationPages").then(({ BonusPoolPage: page }) => ({ default: page })));
const CompensationDashboardPage = lazy(() => import("../features/compensation/pages/CompensationPages").then(({ CompensationDashboardPage: page }) => ({ default: page })));
const CompensationSalaryPage = lazy(() => import("../features/compensation/pages/CompensationPages").then(({ CompensationSalaryPage: page }) => ({ default: page })));
const PerformanceIndicatorPage = lazy(() => import("../features/compensation/pages/CompensationPages").then(({ PerformanceIndicatorPage: page }) => ({ default: page })));
const PerformancePlanPage = lazy(() => import("../features/compensation/pages/PerformanceWorkflowPages").then(({ PerformancePlanPage: page }) => ({ default: page })));
const PerformanceScorePage = lazy(() => import("../features/compensation/pages/PerformanceWorkflowPages").then(({ PerformanceScoreWorkflowPage: page }) => ({ default: page })));
const SalaryDeductionPage = lazy(() => import("../features/compensation/pages/CompensationPages").then(({ SalaryDeductionPage: page }) => ({ default: page })));
const DailyWorkReportPage = lazy(() => import("../features/project/pages/ProjectPages").then(({ DailyWorkReportPage: page }) => ({ default: page })));
const DocumentArchivePage = lazy(() => import("../features/project/pages/ProjectPages").then(({ DocumentArchivePage: page }) => ({ default: page })));
const MachineryPage = lazy(() => import("../features/project/pages/ProjectPages").then(({ MachineryPage: page }) => ({ default: page })));
const MaterialRecordPage = lazy(() => import("../features/project/pages/ProjectPages").then(({ MaterialRecordPage: page }) => ({ default: page })));
const ProjectCostPage = lazy(() => import("../features/project/pages/ProjectPages").then(({ ProjectCostPage: page }) => ({ default: page })));
const ProjectContractItemPage = lazy(() => import("../features/project/pages/ProjectPages").then(({ ProjectContractItemPage: page }) => ({ default: page })));
const ProjectDashboardPage = lazy(() => import("../features/project/pages/ProjectPages").then(({ ProjectDashboardPage: page }) => ({ default: page })));
const ProjectLedgerPage = lazy(() => import("../features/project/pages/ProjectPages").then(({ ProjectLedgerPage: page }) => ({ default: page })));
const ProjectMemberPage = lazy(() => import("../features/project/pages/ProjectPages").then(({ ProjectMemberPage: page }) => ({ default: page })));
const ProjectPaymentPage = lazy(() => import("../features/project/pages/ProjectPages").then(({ ProjectPaymentPage: page }) => ({ default: page })));
const ProjectRiskPage = lazy(() => import("../features/project/pages/ProjectPages").then(({ ProjectRiskPage: page }) => ({ default: page })));
const ProjectSectionPage = lazy(() => import("../features/project/pages/ProjectPages").then(({ ProjectSectionPage: page }) => ({ default: page })));
const TeamPage = lazy(() => import("../features/project/pages/ProjectPages").then(({ TeamPage: page }) => ({ default: page })));
const TeamSettlementPage = lazy(() => import("../features/project/pages/ProjectPages").then(({ TeamSettlementPage: page }) => ({ default: page })));
const TeamUnitPricePage = lazy(() => import("../features/project/pages/ProjectPages").then(({ TeamUnitPricePage: page }) => ({ default: page })));
const VariationOrderPage = lazy(() => import("../features/project/pages/ProjectPages").then(({ VariationOrderPage: page }) => ({ default: page })));
const WorkConfirmationPage = lazy(() => import("../features/project/pages/ProjectPages").then(({ WorkConfirmationPage: page }) => ({ default: page })));
const WorkProcessPage = lazy(() => import("../features/project/pages/ProjectPages").then(({ WorkProcessPage: page }) => ({ default: page })));
const WechatBindingPage = lazy(() => import("../features/system/pages/WechatBindingPage").then(({ WechatBindingPage: page }) => ({ default: page })));
const SystemUserPage = lazy(() => import("../features/system/pages/SystemUserPage").then(({ SystemUserPage: page }) => ({ default: page })));
const OperationLogPage = lazy(() => import("../features/system/pages/OperationLogPage").then(({ OperationLogPage: page }) => ({ default: page })));

function lazyElement(element: ReactNode) {
  return <Suspense fallback={<RouteLoadingState />}>{element}</Suspense>;
}

const createRouter = isElectronRuntime() ? createHashRouter : createBrowserRouter;

export const router = createRouter([
  { path: "/login", element: lazyElement(<LoginPage />) },
  {
    path: "/",
    element: <AppLayout />,
    children: [
      { index: true, element: <Navigate to="/dashboard" replace /> },
      { path: "dashboard", element: lazyElement(<DashboardPage />) },
      { path: "employees", element: lazyElement(<EmployeeListPage />) },
      { path: "employees/create", element: lazyElement(<EmployeeFormPage />) },
      { path: "employees/:id", element: lazyElement(<EmployeeDetailPage />) },
      { path: "employees/:id/edit", element: lazyElement(<EmployeeFormPage />) },
      { path: "employees/:id/salary-history", element: lazyElement(<EmployeeSalaryHistoryPage />) },
      { path: "salary/employees/:id/history", element: lazyElement(<EmployeeSalaryHistoryPage />) },
      { path: "departments", element: lazyElement(<DepartmentPage />) },
      { path: "positions", element: lazyElement(<PositionPage />) },
      { path: "salary/items", element: lazyElement(<SalaryItemPage />) },
      { path: "salary/profiles", element: lazyElement(<SalaryProfilePage />) },
      { path: "salary/monthly", element: lazyElement(<SalaryMonthlyListPage />) },
      { path: "salary/monthly/create", element: lazyElement(<SalaryMonthlyCreatePage />) },
      { path: "salary/monthly/:id", element: lazyElement(<SalaryMonthlyDetailPage />) },
      { path: "salary/monthly/:id/edit", element: lazyElement(<SalaryMonthlyDetailPage />) },
      { path: "salary/monthly/:id/export", element: lazyElement(<SalaryMonthlyExportPage />) },
      { path: "salary/monthly/:id/payslip", element: lazyElement(<SalaryMonthlyPayslipPage />) },
      { path: "salary/monthly/:id/payment", element: lazyElement(<SalaryMonthlyPaymentPage />) },
      { path: "statistics/salary", element: lazyElement(<SalaryStatisticsPage />) },
      { path: "statistics/salary/anomalies", element: lazyElement(<SalaryAnomaliesPage />) },
      { path: "projects", element: lazyElement(<ProjectLedgerPage />) },
      { path: "projects/members", element: lazyElement(<ProjectMemberPage />) },
      { path: "projects/sections", element: lazyElement(<ProjectSectionPage />) },
      { path: "projects/processes", element: lazyElement(<WorkProcessPage />) },
      { path: "projects/contract-items", element: lazyElement(<ProjectContractItemPage />) },
      { path: "projects/daily-reports", element: lazyElement(<DailyWorkReportPage />) },
      { path: "projects/work-confirmation", element: lazyElement(<WorkConfirmationPage />) },
      { path: "projects/teams", element: lazyElement(<TeamPage />) },
      { path: "projects/team-prices", element: lazyElement(<TeamUnitPricePage />) },
      { path: "projects/team-settlements", element: lazyElement(<TeamSettlementPage />) },
      { path: "projects/variations", element: lazyElement(<VariationOrderPage />) },
      { path: "projects/payments", element: lazyElement(<ProjectPaymentPage />) },
      { path: "projects/costs", element: lazyElement(<ProjectCostPage />) },
      { path: "projects/machinery", element: lazyElement(<MachineryPage />) },
      { path: "projects/materials", element: lazyElement(<MaterialRecordPage />) },
      { path: "projects/documents", element: lazyElement(<DocumentArchivePage />) },
      { path: "projects/risks", element: lazyElement(<ProjectRiskPage />) },
      { path: "projects/dashboard", element: lazyElement(<ProjectDashboardPage />) },
      { path: "compensation/attendance", element: lazyElement(<AttendancePage />) },
      { path: "compensation/deductions", element: lazyElement(<SalaryDeductionPage />) },
      { path: "compensation/indicators", element: lazyElement(<PerformanceIndicatorPage />) },
      { path: "compensation/plans", element: lazyElement(<PerformancePlanPage />) },
      { path: "compensation/scores", element: lazyElement(<PerformanceScorePage />) },
      { path: "compensation/bonus-pools", element: lazyElement(<BonusPoolPage />) },
      { path: "compensation/bonus-allocations", element: lazyElement(<BonusAllocationPage />) },
      { path: "compensation/salary", element: lazyElement(<CompensationSalaryPage />) },
      { path: "compensation/dashboard", element: lazyElement(<CompensationDashboardPage />) },
      { path: "system/users", element: lazyElement(<SystemUserPage />) },
      { path: "system/wechat-bindings", element: lazyElement(<WechatBindingPage />) },
      { path: "system/roles", element: lazyElement(<SimpleCrudPage title="角色管理" resource="system/roles" readonly />) },
      { path: "system/logs", element: lazyElement(<OperationLogPage />) },
      { path: "*", element: <NotFoundPage /> }
    ]
  }
]);
