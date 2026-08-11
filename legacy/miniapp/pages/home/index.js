const salary = require("../../services/salary");
const project = require("../../services/project");
const todo = require("../../services/todo");
const auth = require("../../utils/auth");
const format = require("../../utils/format");
const logger = require("../../utils/logger");

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
    variationCount: 0,
    loadError: ""
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
      bindText: user.employeeId ? "已绑定员工" : "未绑定员工",
      bindTagType: user.employeeId ? "primary" : "warning"
    });
    this.load();
  },

  onPullDownRefresh() {
    this.load().finally(() => wx.stopPullDownRefresh());
  },

  async load() {
    this.setData({ loading: true, loadError: "" });
    // Track per-source failures so a total outage shows a banner instead of a
    // silently blank page. Each source still degrades to an empty/default value
    // so one failure does not break the whole screen.
    const failures = [];
    const orLog = (scope, fallback) => (err) => {
      failures.push(scope);
      logger.warn("home." + scope, err);
      return fallback;
    };
    try {
      const today = format.today();
      const [history, projects, todos, reports, variations] = await Promise.all([
        this.data.user && this.data.user.employeeId
          ? salary.myHistory().catch(orLog("salary", null))
          : Promise.resolve(null),
        project.projects({ status: "in_progress" }).catch(orLog("projects", { records: [] })),
        this.data.isManager ? todo.list().catch(orLog("todos", { records: [], total: 0 })) : Promise.resolve({ records: [], total: 0 }),
        project.myDailyReports({ pageSize: 20 }).catch(orLog("reports", { records: [] })),
        project.myVariationOrders({ pageSize: 20 }).catch(orLog("variations", { records: [] }))
      ]);
      const todoRows = format.pageRecords(todos);
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
        todos: todoRows.slice(0, 4),
        todoCount: Number(todos.total || todoRows.length),
        draftCount: reportRows.filter((item) => item.status === "draft").length,
        rejectedCount: reportRows.filter((item) => item.status === "rejected").length + variationRows.filter((item) => item.status === "rejected").length,
        variationCount: variationRows.length
      });
      // If every source failed, surface a banner; a partial outage still shows
      // whatever data we have plus a less alarming toast.
      if (failures.length === 5) {
        this.setData({ loadError: "数据加载失败，请检查网络后下拉刷新重试。" });
      } else if (failures.length > 0) {
        wx.showToast({ title: "部分数据加载失败", icon: "none" });
      }
    } finally {
      this.setData({ loading: false });
    }
  },

  goSalary() {
    wx.switchTab({ url: "/pages/salary/index" });
  },

  retry() {
    this.load();
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

  goSite() {
    wx.switchTab({ url: "/pages/site/index" });
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
