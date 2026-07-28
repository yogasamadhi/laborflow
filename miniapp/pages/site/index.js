const project = require("../../services/project");
const format = require("../../utils/format");

Page({
  data: {
    category: "report",
    categoryNames: ["日报", "签证"],
    status: "",
    statusLabel: "全部状态",
    statusNames: ["全部状态", "草稿", "已提交", "已驳回", "已确认"],
    statusValues: ["", "draft", "submitted", "rejected", "confirmed"],
    keyword: "",
    date: "",
    rows: [],
    page: 1,
    pageSize: 20,
    hasMore: true,
    loading: false,
    loadingMore: false,
    hasLoaded: false,
    loadError: "",
    activeDetail: null
  },

  onShow() {
    this.load(true);
  },

  onPullDownRefresh() {
    this.load(true).finally(() => wx.stopPullDownRefresh());
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading && !this.data.loadingMore) this.load(false);
  },

  onCategoryChange(event) {
    this.setData({ category: Number(event.detail.value) === 0 ? "report" : "variation" }, () => this.load(true));
  },

  onStatusChange(event) {
    const index = Number(event.detail.value);
    this.setData({ status: this.data.statusValues[index], statusLabel: this.data.statusNames[index] }, () => this.load(true));
  },

  onKeyword(event) {
    const value = event.detail && event.detail.value !== undefined ? event.detail.value : event.detail;
    this.setData({ keyword: value });
  },

  onDate(event) {
    this.setData({ date: event.detail.value }, () => this.load(true));
  },

  resetFilters() {
    this.setData({ keyword: "", date: "", status: "", statusLabel: "全部状态" }, () => this.load(true));
  },

  async load(reset) {
    let nextPage = reset ? 1 : this.data.page + 1;
    this.setData(reset ? { loading: true, loadError: "" } : { loadingMore: true, loadError: "" });
    try {
      let records = [];
      let incoming = [];
      do {
        const params = { page: nextPage, pageSize: this.data.pageSize, status: this.data.status || undefined };
        const result = this.data.category === "report"
          ? await project.myDailyReports(params)
          : await project.myVariationOrders(params);
        records = format.pageRecords(result);
        const details = await Promise.all(records.map((row) => this.data.category === "report"
          ? project.dailyReportDetail(row.id)
          : project.variationOrderDetail(row.id)));
        incoming = records.map((row, index) => this.mapRow(row, details[index]))
          .filter((row) => !this.data.date || row.dateText === this.data.date)
          .filter((row) => !this.data.keyword || row.searchText.indexOf(String(this.data.keyword).trim()) >= 0);
        if (incoming.length === 0 && records.length === this.data.pageSize) nextPage += 1;
      } while (incoming.length === 0 && records.length === this.data.pageSize);
      this.setData({
        rows: reset ? incoming : this.data.rows.concat(incoming),
        page: nextPage,
        hasMore: records.length === this.data.pageSize,
        hasLoaded: true
      });
    } catch (error) {
      const message = error.message || "现场记录加载失败";
      this.setData({ loadError: message, hasLoaded: true });
      wx.showToast({ title: message, icon: "none" });
    } finally {
      this.setData({ loading: false, loadingMore: false });
    }
  },

  mapRow(row, detail) {
    if (this.data.category === "report") {
      return Object.assign({}, row, {
        titleText: detail.projectName + " / " + detail.workProcessName,
        subText: detail.sectionName + " · " + String(row.completedQuantity || 0) + (row.unit || ""),
        dateText: row.workDate || "-",
        pricingText: row.pricingStatus === "unpriced" ? "未定价草稿" : (row.pricingBasis || "已定价"),
        amountText: format.money(row.outputValue),
        searchText: [detail.projectName, detail.sectionName, detail.workProcessName, row.reporterName].join(" "),
        statusText: format.status(row.status),
        statusTagType: format.statusTagType(row.status),
        canEdit: row.status === "draft" || row.status === "rejected",
        detailRows: [
          { label: "项目", value: detail.projectName || "-" },
          { label: "施工段", value: detail.sectionName || "-" },
          { label: "工序", value: detail.workProcessName || "-" },
          { label: "班组", value: detail.teamName || "-" },
          { label: "工作日期", value: row.workDate || "-" },
          { label: "工程量", value: String(row.completedQuantity || 0) + (row.unit || "") },
          { label: "单价", value: format.money(row.unitPriceSnapshot) },
          { label: "产值", value: format.money(row.outputValue) },
          { label: "定价依据", value: row.pricingBasis || "未定价" },
          { label: "附件", value: (detail.files || []).map((binding) => binding.file && binding.file.originalName).filter(Boolean).join("、") || "无" }
        ]
      });
    }
    return Object.assign({}, row, {
      titleText: detail.projectName + " / " + (row.variationType || "签证"),
      subText: detail.sectionName + " · " + (row.reason || "未填写原因"),
      dateText: row.occurrenceDate || "-",
      pricingText: row.confirmedAmount == null ? "待确认金额" : "确认金额",
      amountText: format.money(row.confirmedAmount == null ? row.estimatedAmount : row.confirmedAmount),
      searchText: [detail.projectName, detail.sectionName, row.variationType, row.reason].join(" "),
      statusText: format.status(row.status),
      statusTagType: format.statusTagType(row.status),
      canEdit: row.status === "draft" || row.status === "rejected",
      detailRows: [
        { label: "项目", value: detail.projectName || "-" },
        { label: "施工段", value: detail.sectionName || "-" },
        { label: "签证类型", value: row.variationType || "-" },
        { label: "发生日期", value: row.occurrenceDate || "-" },
        { label: "原因", value: row.reason || "-" },
        { label: "预估金额", value: format.money(row.estimatedAmount) },
        { label: "确认金额", value: row.confirmedAmount == null ? "尚未确认" : format.money(row.confirmedAmount) },
        { label: "附件", value: (detail.files || []).map((binding) => binding.file && binding.file.originalName).filter(Boolean).join("、") || "无" }
      ]
    });
  },

  viewDetail(event) {
    this.setData({ activeDetail: event.currentTarget.dataset.item });
  },

  closeDetail() {
    this.setData({ activeDetail: null });
  },

  create() {
    wx.navigateTo({ url: this.data.category === "report" ? "/pages/report/create" : "/pages/variation/create" });
  },

  edit(event) {
    const row = event.currentTarget.dataset.item;
    const path = this.data.category === "report" ? "/pages/report/create?editId=" : "/pages/variation/create?editId=";
    wx.navigateTo({ url: path + row.id });
  }
});
