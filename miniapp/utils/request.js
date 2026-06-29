const env = require("../config/env");
const auth = require("./auth");

function request(options) {
  const token = auth.getToken();
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
        if (res.statusCode === 401 || res.statusCode === 403) {
          auth.clearLogin();
          wx.reLaunch({ url: "/pages/login/index" });
          reject(new Error("登录已失效"));
          return;
        }
        const body = res.data || {};
        if (res.statusCode >= 200 && res.statusCode < 300 && body.code === 0) {
          resolve(body.data);
          return;
        }
        reject(new Error(body.message || "请求失败"));
      },
      fail(err) {
        reject(new Error(err.errMsg || "网络异常"));
      }
    });
  });
}

function get(url, data) {
  return request({ url, data, method: "GET" });
}

function post(url, data) {
  return request({ url, data, method: "POST" });
}

function put(url, data) {
  return request({ url, data, method: "PUT" });
}

module.exports = {
  request,
  get,
  post,
  put
};
