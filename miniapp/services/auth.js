const request = require("../utils/request");

function login(data) {
  return request.post("/auth/login", data);
}

function wechatSession(code) {
  return request.post("/auth/wechat/session", { code });
}

function me() {
  return request.get("/auth/me");
}

module.exports = {
  login,
  wechatSession,
  me
};
