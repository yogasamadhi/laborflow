const TOKEN_KEY = "laborflow_token";
const USER_KEY = "laborflow_user";

function getToken() {
  return wx.getStorageSync(TOKEN_KEY) || "";
}

function getUser() {
  return wx.getStorageSync(USER_KEY) || null;
}

function setLogin(payload) {
  const token = payload.token || "";
  const user = {
    userId: payload.userId,
    employeeId: payload.employeeId,
    username: payload.username,
    nickname: payload.nickname,
    roles: payload.roles || []
  };
  if (token) wx.setStorageSync(TOKEN_KEY, token);
  wx.setStorageSync(USER_KEY, user);
  return user;
}

function clearLogin() {
  wx.removeStorageSync(TOKEN_KEY);
  wx.removeStorageSync(USER_KEY);
}

function hasAnyRole(expected) {
  const user = getUser();
  const roles = user && user.roles ? user.roles : [];
  return expected.some((role) => roles.indexOf(role) >= 0);
}

function isManager() {
  return hasAnyRole(["ADMIN", "BOSS", "FINANCE", "MANAGER"]);
}

module.exports = {
  getToken,
  getUser,
  setLogin,
  clearLogin,
  hasAnyRole,
  isManager
};
