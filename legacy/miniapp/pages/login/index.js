const authService = require("../../services/auth");
const auth = require("../../utils/auth");
const logger = require("../../utils/logger");

Page({
  data: {
    username: "",
    password: "",
    wechatOpenid: "",
    wechatBound: false,
    nickname: "",
    loading: false
  },

  onLoad() {
    this.refreshWechatSession(false).catch((err) => logger.warn("login.session", err));
  },

  refreshWechatSession(showError) {
    return new Promise((resolve, reject) => {
      try {
        wx.login({
          success: async (res) => {
            if (!res.code) {
              const error = new Error("微信登录凭证获取失败");
              if (showError) wx.showToast({ title: error.message, icon: "none" });
              reject(error);
              return;
            }
            try {
              const session = await authService.wechatSession(res.code);
              this.setData({
                wechatOpenid: session.openid || "",
                wechatBound: !!session.bound
              });
              resolve(session);
            } catch (error) {
              if (showError) wx.showToast({ title: error.message, icon: "none" });
              reject(error);
            }
          },
          fail: () => {
            const error = new Error("微信登录凭证获取失败");
            if (showError) wx.showToast({ title: error.message, icon: "none" });
            reject(error);
          }
        });
      } catch (error) {
        if (showError) wx.showToast({ title: error.message || "微信登录凭证获取失败", icon: "none" });
        reject(error);
      }
    });
  },

  onInput(event) {
    const key = event.currentTarget.dataset.key;
    const value = event.detail && event.detail.value !== undefined ? event.detail.value : event.detail;
    this.setData({ [key]: value });
  },

  async submit() {
    const username = this.data.username.trim();
    const password = this.data.password.trim();
    if (!username || !password) {
      wx.showToast({ title: "请输入账号密码", icon: "none" });
      return;
    }
    this.setData({ loading: true });
    try {
      try {
        await this.refreshWechatSession(false);
      } catch (error) {
        wx.showToast({ title: error.message, icon: "none" });
      }
      const data = await authService.login({
        username,
        password,
        wechatOpenid: this.data.wechatOpenid,
        wechatNickname: this.data.nickname
      });
      const user = auth.setLogin(data);
      getApp().globalData.user = user;
      wx.switchTab({ url: "/pages/home/index" });
    } catch (error) {
      wx.showToast({ title: error.message, icon: "none" });
    } finally {
      this.setData({ loading: false });
    }
  }
});
