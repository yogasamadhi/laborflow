const todo = require("../../services/todo");
const auth = require("../../utils/auth");
const format = require("../../utils/format");

Page({
  data: {
    rows: [],
    isManager: false,
    remark: "",
    loading: false
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
    this.setData({ loading: true });
    try {
      const rows = await todo.list();
      this.setData({
        rows: rows.map((item) => Object.assign({}, item, {
          statusText: format.status(item.status)
        }))
      });
    } catch (error) {
      wx.showToast({ title: error.message, icon: "none" });
    } finally {
      this.setData({ loading: false });
    }
  },

  onRemark(event) {
    const value = event.detail && event.detail.value !== undefined ? event.detail.value : event.detail;
    this.setData({ remark: value });
  },

  approve(event) {
    const item = event.currentTarget.dataset.item;
    wx.showModal({
      title: "确认同意",
      content: item.title,
      success: async (res) => {
        if (!res.confirm) return;
        await this.doAction("approve", item);
      }
    });
  },

  reject(event) {
    const item = event.currentTarget.dataset.item;
    wx.showModal({
      title: "确认驳回",
      content: item.title,
      success: async (res) => {
        if (!res.confirm) return;
        await this.doAction("reject", item);
      }
    });
  },

  async doAction(action, item) {
    this.setData({ loading: true });
    try {
      if (action === "approve") {
        await todo.approve(item, this.data.remark);
      } else {
        await todo.reject(item, this.data.remark);
      }
      wx.showToast({ title: "已处理", icon: "success" });
      this.setData({ remark: "" });
      await this.load();
    } catch (error) {
      wx.showToast({ title: error.message, icon: "none" });
    } finally {
      this.setData({ loading: false });
    }
  }
});
