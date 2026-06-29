const authService = require("../../services/auth");
const auth = require("../../utils/auth");

Page({
  data: {
    username: "",
    password: "",
    wxCode: "",
    nickname: "",
    authButtonText: "微信授权",
    loading: false
  },

  onLoad() {
    wx.login({
      success: (res) => {
        this.setData({ wxCode: res.code || "" });
      }
    });
  },

  onInput(event) {
    const key = event.currentTarget.dataset.key;
    const value = event.detail && event.detail.value !== undefined ? event.detail.value : event.detail;
    this.setData({ [key]: value });
  },

  chooseProfile() {
    wx.getUserProfile({
      desc: "用于绑定劳动工资平台账号",
      success: (res) => {
        const nickname = res.userInfo.nickName || "";
        this.setData({ nickname, authButtonText: nickname ? "已授权：" + nickname : "微信授权" });
      }
    });
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
      const data = await authService.login({
        username,
        password,
        wechatCode: this.data.wxCode,
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
