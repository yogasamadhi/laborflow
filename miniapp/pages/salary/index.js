const salary = require("../../services/salary");
const auth = require("../../utils/auth");
const format = require("../../utils/format");

Page({
  data: {
    history: null,
    records: [],
    employeeNameText: "员工",
    totalActualText: "¥0.00",
    unbound: false,
    loading: false,
    hasLoaded: false,
    loadError: ""
  },

  onShow() {
    const user = auth.getUser();
    if (!user) {
      wx.reLaunch({ url: "/pages/login/index" });
      return;
    }
    if (!user.employeeId) {
      this.setData({
        unbound: true,
        employeeNameText: "未绑定员工",
        totalActualText: "¥0.00",
        records: [],
        hasLoaded: true,
        loadError: ""
      });
      return;
    }
    this.setData({ unbound: false });
    this.load();
  },

  onPullDownRefresh() {
    this.load().finally(() => wx.stopPullDownRefresh());
  },

  async load() {
    this.setData({ loading: true, loadError: "" });
    try {
      const history = await salary.myHistory();
      const records = (history.records || []).map((item) => Object.assign({}, item, {
        actualAmountText: format.money(item.actualAmount),
        payableAmountText: format.money(item.payableAmount),
        deductionAmountText: format.money(item.deductionAmount),
        paymentDateText: item.paymentDate || "未发放",
        statusText: format.status(item.paymentStatus),
        statusTagType: item.paymentStatus === "paid" ? "primary" : "warning",
        payslipStatusText: payslipStatusText(item.payslipStatus),
        payslipStatusTagType: item.payslipStatus === "confirmed" ? "primary" : "warning"
      }));
      this.setData({ history, records });
      this.setData({
        employeeNameText: history.employeeName || "员工",
        totalActualText: format.money(history.totalActualAmount),
        hasLoaded: true
      });
    } catch (error) {
      const message = error.message || "工资记录加载失败";
      this.setData({ loadError: message, hasLoaded: true });
      wx.showToast({ title: message, icon: "none" });
    } finally {
      this.setData({ loading: false });
    }
  },

  openDetail(event) {
    const item = event.currentTarget.dataset.item;
    wx.navigateTo({
      url: "/pages/salary/detail?detailId=" + item.detailId + "&monthlyId=" + item.monthlyId + "&month=" + item.salaryMonth
    });
  }
});

function payslipStatusText(status) {
  if (status === "confirmed") return "工资条已确认";
  if (status === "exception") return "工资条有异常";
  return "待确认工资条";
}
