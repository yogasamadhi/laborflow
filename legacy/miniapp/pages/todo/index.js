const todo = require("../../services/todo");
const auth = require("../../utils/auth");
const format = require("../../utils/format");

Page({
  data: {
    rows: [],
    isManager: false,
    loading: false,
    loadingMore: false,
    loadError: "",
    hasLoaded: false,
    page: 1,
    pageSize: 20,
    hasMore: true,
    activeItem: null,
    detailRows: [],
    detailLoading: false,
    actionMode: "",
    actionRemark: "",
    confirmedAmount: "",
    processing: false
  },

  onShow() {
    const isManager = auth.isManager();
    this.setData({ isManager });
    if (isManager) this.load(true);
  },

  onPullDownRefresh() {
    this.load(true).finally(() => wx.stopPullDownRefresh());
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading && !this.data.loadingMore) this.load(false);
  },

  async load(reset) {
    if (!this.data.isManager) return;
    const nextPage = reset ? 1 : this.data.page + 1;
    this.setData(reset ? { loading: true, loadError: "" } : { loadingMore: true, loadError: "" });
    try {
      const result = await todo.list({ page: nextPage, pageSize: this.data.pageSize });
      const incoming = (result.records || []).map((item) => Object.assign({}, item, {
        statusText: format.status(item.status),
        statusTagType: format.statusTagType(item.status),
        occurredAtText: item.occurredAt ? item.occurredAt.replace("T", " ").slice(0, 16) : "-"
      }));
      this.setData({
        rows: reset ? incoming : this.data.rows.concat(incoming),
        page: nextPage,
        hasMore: incoming.length === this.data.pageSize,
        hasLoaded: true
      });
    } catch (error) {
      const message = error.message || "待办加载失败";
      this.setData({ loadError: message, hasLoaded: true });
      wx.showToast({ title: message, icon: "none" });
    } finally {
      this.setData({ loading: false, loadingMore: false });
    }
  },

  async openDetail(event) {
    if (this.data.detailLoading || this.data.processing) return;
    const item = event.currentTarget.dataset.item;
    this.setData({ activeItem: item, detailRows: [], detailLoading: true, actionMode: "", actionRemark: "", confirmedAmount: "" });
    try {
      const detail = await todo.detail(item);
      this.setData({ detailRows: this.mapDetail(item, detail) });
    } catch (error) {
      wx.showToast({ title: error.message || "详情加载失败", icon: "none" });
      this.setData({ activeItem: null });
    } finally {
      this.setData({ detailLoading: false });
    }
  },

  mapDetail(item, detail) {
    if (item.type === "salary") {
      return [
        { label: "工资月份", value: detail.header.salaryMonth || "-" },
        { label: "工资人数", value: String(detail.header.employeeCount || 0) },
        { label: "应发合计", value: format.money(detail.header.payableAmount) },
        { label: "扣款合计", value: format.money(detail.header.deductionAmount) },
        { label: "实发合计", value: format.money(detail.header.actualAmount) },
        { label: "审批记录", value: String((detail.audits || []).length) + " 条" }
      ];
    }
    if (item.type === "report_pm" || item.type === "report_final") {
      const row = detail.report || {};
      return [
        { label: "项目", value: detail.projectName || "-" },
        { label: "施工段", value: detail.sectionName || "-" },
        { label: "工序", value: detail.workProcessName || "-" },
        { label: "工作日期", value: row.workDate || "-" },
        { label: "工程量", value: String(row.completedQuantity || 0) + (row.unit || "") },
        { label: "定价依据", value: row.pricingBasis || (row.pricingStatus === "unpriced" ? "未定价" : "-") },
        { label: "单价", value: format.money(row.unitPriceSnapshot) },
        { label: "产值", value: format.money(row.outputValue) },
        { label: "附件", value: String((detail.files || []).length) + " 个" }
      ];
    }
    if (item.type === "variation") {
      const row = detail.order || {};
      return [
        { label: "项目", value: detail.projectName || "-" },
        { label: "施工段", value: detail.sectionName || "-" },
        { label: "签证类型", value: row.variationType || "-" },
        { label: "发生日期", value: row.occurrenceDate || "-" },
        { label: "原因", value: row.reason || "-" },
        { label: "预估金额", value: format.money(row.estimatedAmount) },
        { label: "附件", value: String((detail.files || []).length) + " 个" }
      ];
    }
    const header = detail.settlement || {};
    return [
      { label: "班组", value: header.teamName || "-" },
      { label: "结算月份", value: header.settlementMonth || "-" },
      { label: "确认工程量", value: String(header.confirmedQuantity || 0) },
      { label: "结算金额", value: format.money(header.settlementAmount) },
      { label: "实付金额", value: format.money(header.actualPayAmount) },
      { label: "明细行", value: String((detail.details || []).length) + " 条" }
    ];
  },

  closeDetail() {
    if (!this.data.processing) this.setData({ activeItem: null, detailRows: [], actionMode: "" });
  },

  beginAction(event) {
    this.setData({ actionMode: event.currentTarget.dataset.action, actionRemark: "", confirmedAmount: "" });
  },

  onActionRemark(event) {
    const value = event.detail && event.detail.value !== undefined ? event.detail.value : event.detail;
    this.setData({ actionRemark: value });
  },

  onConfirmedAmount(event) {
    const value = event.detail && event.detail.value !== undefined ? event.detail.value : event.detail;
    this.setData({ confirmedAmount: value });
  },

  async confirmAction() {
    const item = this.data.activeItem;
    const remark = String(this.data.actionRemark || "").trim();
    if (!item || this.data.processing) return;
    if (this.data.actionMode === "reject" && !remark) {
      wx.showToast({ title: "驳回时请填写处理原因", icon: "none" });
      return;
    }
    if (this.data.actionMode === "approve" && item.type === "variation" && this.data.confirmedAmount === "") {
      wx.showToast({ title: "请输入确认金额", icon: "none" });
      return;
    }
    this.setData({ processing: true });
    try {
      if (this.data.actionMode === "approve") {
        await todo.approve(item, { remark, confirmedAmount: this.data.confirmedAmount });
      } else {
        await todo.reject(item, remark);
      }
      wx.showToast({ title: this.data.actionMode === "approve" ? "已同意" : "已驳回", icon: "success" });
      this.setData({ activeItem: null, detailRows: [], actionMode: "" });
      await this.load(true);
    } catch (error) {
      wx.showToast({ title: error.message || "处理失败，请重试", icon: "none" });
    } finally {
      this.setData({ processing: false });
    }
  }
});
