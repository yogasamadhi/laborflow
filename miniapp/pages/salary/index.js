const salary = require("../../services/salary");
const auth = require("../../utils/auth");
const format = require("../../utils/format");

Page({
  data: {
    history: null,
    records: [],
    employeeNameText: "员工",
    totalActualText: "0.00",
    unbound: false,
    loading: false
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
        totalActualText: "0.00",
        records: []
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
    this.setData({ loading: true });
    try {
      const history = await salary.myHistory();
      const records = (history.records || []).map((item) => Object.assign({}, item, {
        actualAmountText: format.money(item.actualAmount),
        payableAmountText: format.money(item.payableAmount),
        deductionAmountText: format.money(item.deductionAmount),
        paymentDateText: item.paymentDate || "未发放",
        statusText: format.status(item.paymentStatus),
        statusTagType: item.paymentStatus === "paid" ? "primary" : "warning"
      }));
      this.setData({ history, records });
      this.setData({
        employeeNameText: history.employeeName || "员工",
        totalActualText: history.totalActualAmount || "0.00"
      });
    } catch (error) {
      wx.showToast({ title: error.message, icon: "none" });
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
