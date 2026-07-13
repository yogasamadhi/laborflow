const request = require("../utils/request");

function login(data) {
  return request.post("/auth/login", data, { skipAuth: true, skipSessionRecovery: true });
}

function wechatSession(code) {
  return request.post("/auth/wechat/session", { code }, { skipAuth: true });
}

function me() {
  return request.get("/auth/me");
}

function logout() {
  return request.post("/auth/logout", {}, { skipSessionRecovery: true });
}

module.exports = {
  login,
  wechatSession,
  me,
  logout
};
