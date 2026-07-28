const authService = require("../../services/auth");
const auth = require("../../utils/auth");

Page({
  data: {
    user: null,
    avatarText: "L",
    displayName: "",
    usernameText: "",
    employeeBindingText: "未绑定",
    employeeBound: false,
    rolesText: "-",
    refreshing: false,
    loggingOut: false
  },

  onShow() {
    const user = auth.getUser();
    if (!user) {
      wx.reLaunch({ url: "/pages/login/index" });
      return;
    }
    this.setProfile(user);
    this.refreshMe();
  },

  async refreshMe() {
    if (this.data.refreshing) return;
    this.setData({ refreshing: true });
    try {
      const data = await authService.me();
      const user = auth.refreshUser(data);
      getApp().globalData.user = user;
      this.setProfile(user);
    } catch (error) {
      wx.showToast({ title: error.message, icon: "none" });
    } finally {
      this.setData({ refreshing: false });
    }
  },

  async logout() {
    if (this.data.loggingOut) return;
    this.setData({ loggingOut: true });
    try {
      await authService.logout();
    } catch (error) {
      wx.showToast({ title: "已清理本地登录信息", icon: "none" });
    } finally {
      auth.clearLogin();
      getApp().globalData.user = null;
      wx.reLaunch({ url: "/pages/login/index" });
    }
  },

  setProfile(user) {
    const displayName = user.nickname || user.username || "";
    this.setData({
      user,
      avatarText: displayName ? displayName.substring(0, 1) : "L",
      displayName,
      usernameText: user.username || "-",
      employeeBindingText: user.employeeId ? "已绑定" : "未绑定",
      employeeBound: !!user.employeeId,
      rolesText: user.roles && user.roles.length ? user.roles.join("、") : "-"
    });
  }
});
