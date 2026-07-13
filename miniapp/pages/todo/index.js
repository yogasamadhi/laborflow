const todo = require("../../services/todo");
const auth = require("../../utils/auth");
const format = require("../../utils/format");

Page({
  data: {
    rows: [],
    isManager: false,
    loading: false,
    loadError: "",
    hasLoaded: false,
    activeItemId: "",
    activeItem: null,
    actionMode: "",
    actionRemark: "",
    processing: false
  },

  onShow() {
    const isManager = auth.isManager();
    this.setData({ isManager });
    if (isManager) this.load();
  },

  onPullDownRefresh() {
    this.load().finally(() => wx.stopPullDownRefresh());
  },

  async load() {
    if (!this.data.isManager) return;
    this.setData({ loading: true, loadError: "" });
    try {
      const rows = await todo.list();
      this.setData({
        rows: rows.map((item) => Object.assign({}, item, {
          statusText: format.status(item.status),
          statusTagType: format.statusTagType(item.status)
        })),
        hasLoaded: true
      });
    } catch (error) {
      const message = error.message || "待办加载失败";
      this.setData({ loadError: message, hasLoaded: true });
      wx.showToast({ title: message, icon: "none" });
    } finally {
      this.setData({ loading: false });
    }
  },

  beginAction(event) {
    if (this.data.loading || this.data.processing) return;
    const item = event.currentTarget.dataset.item;
    const actionMode = event.currentTarget.dataset.action;
    this.setData({
      activeItemId: item.id,
      activeItem: item,
      actionMode,
      actionRemark: ""
    });
  },

  cancelAction() {
    if (this.data.processing) return;
    this.setData({ activeItemId: "", activeItem: null, actionMode: "", actionRemark: "" });
  },

  onActionRemark(event) {
    const value = event.detail && event.detail.value !== undefined ? event.detail.value : event.detail;
    this.setData({ actionRemark: value });
  },

  async confirmAction() {
    const item = this.data.activeItem;
    const remark = String(this.data.actionRemark || "").trim();
    if (!item || this.data.processing) return;
    if (this.data.actionMode === "reject" && !remark) {
      wx.showToast({ title: "驳回时请填写处理原因", icon: "none" });
      return;
    }
    this.setData({ processing: true });
    try {
      if (this.data.actionMode === "approve") {
        await todo.approve(item, remark);
      } else {
        await todo.reject(item, remark);
      }
      wx.showToast({ title: this.data.actionMode === "approve" ? "已同意" : "已驳回", icon: "success" });
      this.setData({ activeItemId: "", activeItem: null, actionMode: "", actionRemark: "" });
      await this.load();
    } catch (error) {
      wx.showToast({ title: error.message || "处理失败，请重试", icon: "none" });
    } finally {
      this.setData({ processing: false });
    }
  }
});
