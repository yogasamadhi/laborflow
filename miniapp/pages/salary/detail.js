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
    feedback: "",
    feedbackOpen: false,
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
          payslipStatusText: format.payslipStatusText(detail.payslipStatus),
          payslipStatusTagType: format.payslipStatusTagType(detail.payslipStatus)
        }) : null;
      this.setData({
        detail: mappedDetail,
        items: mappedItems,
        hasLoaded: true,
        canConfirmPayslip: !!mappedDetail && mappedDetail.payslipStatus !== "confirmed" && mappedDetail.payslipStatus !== "exception",
        payslipNotice: mappedDetail && mappedDetail.payslipStatus === "exception"
          ? "已提出异议：" + (mappedDetail.payslipFeedback || "等待后台处理") + (mappedDetail.payslipFeedbackAt ? "（" + mappedDetail.payslipFeedbackAt.replace("T", " ").slice(0, 16) + "）" : "")
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
  },

  openFeedback() {
    if (this.data.canConfirmPayslip && !this.data.confirming) this.setData({ feedbackOpen: true, feedback: "" });
  },

  cancelFeedback() {
    if (!this.data.confirming) this.setData({ feedbackOpen: false, feedback: "" });
  },

  onFeedback(event) {
    const value = event.detail && event.detail.value !== undefined ? event.detail.value : event.detail;
    this.setData({ feedback: value });
  },

  async submitFeedback() {
    const feedback = String(this.data.feedback || "").trim();
    if (!feedback) {
      wx.showToast({ title: "请填写异议说明", icon: "none" });
      return;
    }
    this.setData({ confirming: true });
    try {
      await salary.feedbackPayslip(this.data.detailId, feedback);
      wx.showToast({ title: "异议已提交", icon: "success" });
      this.setData({ feedbackOpen: false, feedback: "" });
      await this.load();
    } catch (error) {
      wx.showToast({ title: error.message || "异议提交失败", icon: "none" });
    } finally {
      this.setData({ confirming: false });
    }
  }
});
