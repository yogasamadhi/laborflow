const project = require("../../services/project");
const format = require("../../utils/format");

Page({
  data: {
    rows: [],
    loading: false,
    hasLoaded: false,
    loadError: ""
  },

  onShow() {
    this.load();
  },

  onPullDownRefresh() {
    this.load().finally(() => wx.stopPullDownRefresh());
  },

  async load() {
    this.setData({ loading: true, loadError: "" });
    try {
      const data = await project.myDailyReports();
      const rows = format.pageRecords(data).map((item) => Object.assign({}, item, {
        statusText: format.status(item.status),
        statusTagType: item.status === "rejected" ? "danger" : item.status === "draft" ? "warning" : "primary",
        canEdit: item.status === "draft" || item.status === "rejected",
        actionText: item.status === "rejected" ? "修改再提交" : "继续编辑",
        workDateText: item.workDate || "-",
        completedQuantityText: item.completedQuantity || 0,
        unitText: item.unit || "",
        workerCountText: item.workerCount || 0
      }));
      this.setData({ rows, hasLoaded: true });
    } catch (error) {
      const message = error.message || "日报加载失败";
      this.setData({ loadError: message, hasLoaded: true });
      wx.showToast({ title: message, icon: "none" });
    } finally {
      this.setData({ loading: false });
    }
  },

  create() {
    wx.navigateTo({ url: "/pages/report/create" });
  },

  edit(event) {
    const item = event.currentTarget.dataset.item;
    wx.setStorageSync("laborflow_edit_report", item);
    wx.navigateTo({ url: "/pages/report/create?editId=" + item.id });
  }
});
