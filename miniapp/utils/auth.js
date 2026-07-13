const TOKEN_KEY = "laborflow_token";
const USER_KEY = "laborflow_user";

function normalizedText(value) {
  return typeof value === "string" ? value.trim() : "";
}

function normalizedOptionalId(value) {
  return Number.isSafeInteger(value) && value > 0 ? value : null;
}

function normalizeUser(payload) {
  const userId = payload && normalizedOptionalId(payload.userId);
  const username = payload && normalizedText(payload.username);
  if (!userId || !username) return null;

  return {
    userId,
    employeeId: normalizedOptionalId(payload.employeeId),
    username,
    nickname: normalizedText(payload.nickname),
    roles: Array.isArray(payload.roles)
      ? payload.roles.map(normalizedText).filter(Boolean)
      : []
  };
}

function getSession() {
  const token = normalizedText(wx.getStorageSync(TOKEN_KEY));
  const user = normalizeUser(wx.getStorageSync(USER_KEY));
  if (!token || !user) {
    if (token || wx.getStorageSync(USER_KEY)) clearLogin();
    return null;
  }
  return { token, user };
}

function getToken() {
  const session = getSession();
  return session ? session.token : "";
}

function getUser() {
  const session = getSession();
  return session ? session.user : null;
}

function setLogin(payload) {
  const user = normalizeUser(payload);
  const token = normalizedText(payload && payload.token);
  if (!token || !user) {
    clearLogin();
    throw new Error("登录信息不完整，请重新登录");
  }
  wx.setStorageSync(TOKEN_KEY, token);
  wx.setStorageSync(USER_KEY, user);
  return user;
}

function refreshUser(payload) {
  const session = getSession();
  if (!session) {
    throw new Error("登录已失效");
  }
  return setLogin(Object.assign({}, payload, { token: session.token }));
}

function clearLogin() {
  wx.removeStorageSync(TOKEN_KEY);
  wx.removeStorageSync(USER_KEY);
}

function hasAnyRole(expected) {
  const user = getUser();
  const roles = user && user.roles ? user.roles : [];
  return Array.isArray(expected) && expected.some((role) => roles.indexOf(role) >= 0);
}

function isManager() {
  return hasAnyRole(["ADMIN", "BOSS", "FINANCE", "MANAGER"]);
}

module.exports = {
  getSession,
  getToken,
  getUser,
  setLogin,
  refreshUser,
  clearLogin,
  hasAnyRole,
  isManager
};
