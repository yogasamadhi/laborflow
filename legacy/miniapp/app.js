const auth = require("./utils/auth");

App({
  globalData: {
    user: null
  },

  onLaunch() {
    this.globalData.user = auth.getUser();
  }
});
