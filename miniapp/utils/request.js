const env = require("../config/env");
const auth = require("./auth");

let loginRedirecting = false;

function messageOf(body, fallback) {
  return body && typeof body.message === "string" && body.message ? body.message : fallback;
}

function recoverExpiredSession() {
  auth.clearLogin();
  if (loginRedirecting) return;
  loginRedirecting = true;
  wx.reLaunch({
    url: "/pages/login/index",
    complete() {
      loginRedirecting = false;
    }
  });
}

function request(options) {
  const token = options.skipAuth ? "" : auth.getToken();
  const header = Object.assign(
    {
      "content-type": "application/json"
    },
    options.header || {}
  );
  if (token) {
    header.Authorization = "Bearer " + token;
  }

  return new Promise((resolve, reject) => {
    wx.request({
      url: /^https?:\/\//.test(options.url) ? options.url : env.baseURL + options.url,
      method: options.method || "GET",
      data: options.data || {},
      header,
      success(res) {
        const body = res.data || {};
        if (res.statusCode === 401 && token && !options.skipSessionRecovery) {
          recoverExpiredSession();
          reject(new Error("登录已失效"));
          return;
        }
        if (res.statusCode === 403) {
          reject(new Error(messageOf(body, "当前账号无权执行此操作")));
          return;
        }
        if (res.statusCode >= 200 && res.statusCode < 300 && body.code === 0) {
          resolve(body.data);
          return;
        }
        reject(new Error(messageOf(body, "请求失败")));
      },
      fail(err) {
        reject(new Error((err && err.errMsg) || "网络异常，请检查网络后重试"));
      }
    });
  });
}

function get(url, data, options) {
  return request(Object.assign({}, options, { url, data, method: "GET" }));
}

function post(url, data, options) {
  return request(Object.assign({}, options, { url, data, method: "POST" }));
}

function put(url, data, options) {
  return request(Object.assign({}, options, { url, data, method: "PUT" }));
}

module.exports = {
  request,
  get,
  post,
  put,
  recoverExpiredSession
};
