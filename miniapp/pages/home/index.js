const salary = require("../../services/salary");
const project = require("../../services/project");
const todo = require("../../services/todo");
const auth = require("../../utils/auth");
const format = require("../../utils/format");

Page({
  data: {
    user: null,
    projects: [],
    todos: [],
    loading: false,
    isManager: false,
    displayName: "",
    roleText: "",
    bindText: "",
    bindTagType: "warning",
    latestPaymentMonth: "暂无",
    latestPaymentStatus: "暂无工资",
    totalActualText: "0.00",
    todayReport: null,
    todayReportText: "今日未填报",
    todayReportBadgeText: "待填报",
    todayReportTagType: "warning",
    todoCount: 0,
    draftCount: 0,
    rejectedCount: 0,
    variationCount: 0
  },

  onShow() {
    const user = auth.getUser();
    if (!user) {
      wx.reLaunch({ url: "/pages/login/index" });
      return;
    }
    this.setData({
      user,
      isManager: auth.isManager(),
      displayName: user.nickname || user.username || "",
      roleText: this.roleText(user.roles || []),
      bindText: user.employeeId ? "已绑定员工 #" + user.employeeId : "未绑定员工",
      bindTagType: user.employeeId ? "primary" : "warning"
    });
    this.load();
  },

  onPullDownRefresh() {
    this.load().finally(() => wx.stopPullDownRefresh());
  },

  async load() {
    this.setData({ loading: true });
    try {
      const today = format.today();
      const [history, projects, todos, reports, variations] = await Promise.all([
        salary.myHistory().catch(() => null),
        project.projects({ status: "in_progress" }).catch(() => ({ records: [] })),
        todo.list().catch(() => []),
        project.myDailyReports({ pageSize: 50 }).catch(() => ({ records: [] })),
        project.myVariationOrders({ pageSize: 50 }).catch(() => ({ records: [] }))
      ]);
      const reportRows = format.pageRecords(reports);
      const todayReport = reportRows.find((item) => item.workDate === today) || null;
      const variationRows = format.pageRecords(variations);
      this.setData({
        totalActualText: history ? history.totalActualAmount : "0.00",
        projects: format.pageRecords(projects).slice(0, 3).map((item) => Object.assign({}, item, {
          projectManagerText: item.projectManager || "未设置负责人"
        })),
        latestPaymentMonth: history && history.latestPaymentRecord ? history.latestPaymentRecord.salaryMonth : "暂无",
        latestPaymentStatus: history && history.latestPaymentRecord ? format.status(history.latestPaymentRecord.paymentStatus) : "暂无工资",
        todayReport,
        todayReportText: todayReport ? format.status(todayReport.status) : "今日未填报",
        todayReportBadgeText: todayReport ? "已记录" : "待填报",
        todayReportTagType: todayReport ? format.statusTagType(todayReport.status) : "warning",
        todos: todos.slice(0, 4),
        todoCount: todos.length,
        draftCount: reportRows.filter((item) => item.status === "draft").length,
        rejectedCount: reportRows.filter((item) => item.status === "rejected").length + variationRows.filter((item) => item.status === "rejected").length,
        variationCount: variationRows.length
      });
    } finally {
      this.setData({ loading: false });
    }
  },

  goSalary() {
    wx.switchTab({ url: "/pages/salary/index" });
  },

  goReportCreate() {
    wx.navigateTo({ url: "/pages/report/create" });
  },

  goVariationCreate() {
    wx.navigateTo({ url: "/pages/variation/create" });
  },

  goReportPhoto() {
    wx.navigateTo({ url: "/pages/report/create?autoPhoto=1" });
  },

  goTodo() {
    wx.switchTab({ url: "/pages/todo/index" });
  },

  money(value) {
    return format.money(value);
  },

  roleText(roles) {
    if (roles.indexOf("ADMIN") >= 0) return "系统管理员";
    if (roles.indexOf("BOSS") >= 0) return "老板/管理层";
    if (roles.indexOf("FINANCE") >= 0) return "财务";
    if (roles.indexOf("MANAGER") >= 0) return "项目经理/管理人员";
    return "现场/员工";
  }
});
