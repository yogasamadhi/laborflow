const salary = require("../../services/salary");
const format = require("../../utils/format");

Page({
  data: {
    monthlyId: "",
    detailId: "",
    month: "",
    detail: null,
    items: [],
    loading: false,
    confirming: false,
    hasLoaded: false,
    loadError: "",
    canConfirmPayslip: false,
    payslipNotice: ""
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
    this.setData({ loading: true, loadError: "" });
    try {
      const [detail, items] = await Promise.all([
        salary.detail(this.data.detailId),
        salary.detailItems(this.data.detailId)
      ]);
      const mappedItems = (items || []).map((item) => Object.assign({}, item, {
        amountText: format.money(item.amount),
        typeText: item.salaryItemType === "deduction" ? "扣减" : "应发",
        tagType: item.salaryItemType === "deduction" ? "danger" : "primary",
        amountClass: item.salaryItemType === "deduction" ? "deduction" : "money"
      }));
      const mappedDetail = detail ? Object.assign({}, detail, {
          payableAmountText: format.money(detail.payableAmount),
          deductionAmountText: format.money(detail.deductionAmount),
          actualAmountText: format.money(detail.actualAmount),
          paymentDateText: detail.paymentDate || "-",
          paymentStatusText: format.status(detail.paymentStatus),
          paymentStatusTagType: detail.paymentStatus === "paid" ? "primary" : "warning",
          payslipStatusText: payslipStatusText(detail.payslipStatus),
          payslipStatusTagType: detail.payslipStatus === "confirmed" ? "primary" : "warning"
        }) : null;
      this.setData({
        detail: mappedDetail,
        items: mappedItems,
        hasLoaded: true,
        canConfirmPayslip: !!mappedDetail && mappedDetail.payslipStatus !== "confirmed" && mappedDetail.payslipStatus !== "exception",
        payslipNotice: mappedDetail && mappedDetail.payslipStatus === "exception"
          ? "该工资条已标记为异常，请联系管理员核对后再处理。"
          : (mappedDetail && mappedDetail.payslipStatus === "confirmed" ? "本工资条已确认，无需重复操作。" : "确认前请核对工资项目、扣减和实发金额。")
      });
    } catch (error) {
      const message = error.message || "工资条加载失败";
      this.setData({ loadError: message, hasLoaded: true });
      wx.showToast({ title: message, icon: "none" });
    } finally {
      this.setData({ loading: false });
    }
  },

  confirmPayslip() {
    if (!this.data.detail || !this.data.canConfirmPayslip || this.data.confirming) {
      return;
    }
    wx.showModal({
      title: "确认工资条",
      content: "请确认工资项目和金额无误后再提交确认。",
      confirmText: "确认无误",
      success: async (result) => {
        if (!result.confirm) {
          return;
        }
        this.setData({ confirming: true });
        try {
          await salary.confirmPayslip(this.data.detailId);
          wx.showToast({ title: "工资条已确认", icon: "success" });
          await this.load();
        } catch (error) {
          wx.showToast({ title: error.message || "确认失败", icon: "none" });
        } finally {
          this.setData({ confirming: false });
        }
      }
    });
  }
});

function payslipStatusText(status) {
  if (status === "confirmed") return "已确认";
  if (status === "exception") return "有异常";
  return "待确认";
}
