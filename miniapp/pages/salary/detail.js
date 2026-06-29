const salary = require("../../services/salary");
const format = require("../../utils/format");

Page({
  data: {
    monthlyId: "",
    detailId: "",
    month: "",
    detail: null,
    items: [],
    loading: false
  },

  onLoad(query) {
    this.setData({
      monthlyId: query.monthlyId || "",
      detailId: query.detailId || "",
      month: query.month || ""
    });
    this.load();
  },

  async load() {
    this.setData({ loading: true });
    try {
      const [details, items] = await Promise.all([
        salary.monthlyDetails(this.data.monthlyId),
        salary.detailItems(this.data.detailId)
      ]);
      const detail = (details.records || []).find((item) => String(item.id) === String(this.data.detailId)) || null;
      const mappedItems = (items || []).map((item) => Object.assign({}, item, {
        amountText: format.money(item.amount),
        typeText: item.salaryItemType === "deduction" ? "扣减" : "应发",
        tagType: item.salaryItemType === "deduction" ? "danger" : "primary",
        amountClass: item.salaryItemType === "deduction" ? "deduction" : "money"
      }));
      this.setData({
        detail: detail ? Object.assign({}, detail, {
          payableAmountText: format.money(detail.payableAmount),
          deductionAmountText: format.money(detail.deductionAmount),
          actualAmountText: format.money(detail.actualAmount),
          paymentDateText: detail.paymentDate || "-",
          paymentStatusText: format.status(detail.paymentStatus),
          paymentStatusTagType: detail.paymentStatus === "paid" ? "primary" : "warning"
        }) : null,
        items: mappedItems
      });
    } catch (error) {
      wx.showToast({ title: error.message, icon: "none" });
    } finally {
      this.setData({ loading: false });
    }
  }
});
