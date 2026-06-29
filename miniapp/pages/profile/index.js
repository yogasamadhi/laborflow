const authService = require("../../services/auth");
const auth = require("../../utils/auth");

Page({
  data: {
    user: null,
    avatarText: "L",
    displayName: "",
    usernameText: "",
    userIdText: "-",
    employeeIdText: "未绑定",
    employeeBound: false,
    rolesText: "-"
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
    try {
      const data = await authService.me();
      const user = auth.setLogin(Object.assign({}, data, { token: auth.getToken() }));
      getApp().globalData.user = user;
      this.setProfile(user);
    } catch (error) {
      wx.showToast({ title: error.message, icon: "none" });
    }
  },

  logout() {
    auth.clearLogin();
    getApp().globalData.user = null;
    wx.reLaunch({ url: "/pages/login/index" });
  },

  setProfile(user) {
    const displayName = user.nickname || user.username || "";
    this.setData({
      user,
      avatarText: displayName ? displayName.substring(0, 1) : "L",
      displayName,
      usernameText: user.username || "-",
      userIdText: user.userId || "-",
      employeeIdText: user.employeeId || "未绑定",
      employeeBound: !!user.employeeId,
      rolesText: user.roles && user.roles.length ? user.roles.join("、") : "-"
    });
  }
});
