const env = require("../config/env");
const auth = require("../utils/auth");
const request = require("../utils/request");

function upload(path) {
  return new Promise((resolve, reject) => {
    const token = auth.getToken();
    const header = {};
    if (token) header.Authorization = "Bearer " + token;
    wx.uploadFile({
      url: env.uploadURL,
      filePath: path,
      name: "file",
      header,
      success(res) {
        let body = {};
        try {
          body = JSON.parse(res.data);
        } catch (error) {
          reject(new Error("上传结果解析失败"));
          return;
        }
        if (res.statusCode === 401 && token) {
          request.recoverExpiredSession();
          reject(new Error("登录已失效"));
          return;
        }
        if (res.statusCode === 403) {
          reject(new Error(body.message || "当前账号无权上传文件"));
          return;
        }
        if (res.statusCode >= 200 && res.statusCode < 300 && body.code === 0) {
          resolve(body.data);
          return;
        }
        reject(new Error(body.message || "上传失败"));
      },
      fail(err) {
        reject(new Error((err && err.errMsg) || "上传失败，请检查网络后重试"));
      }
    });
  });
}

function chooseAndUpload() {
  return new Promise((resolve, reject) => {
    wx.chooseMedia({
      count: 6,
      mediaType: ["image"],
      sourceType: ["album", "camera"],
      success: async (res) => {
        try {
          const files = [];
          for (const item of res.tempFiles || []) {
            files.push(await upload(item.tempFilePath));
          }
          resolve(files);
        } catch (error) {
          reject(error);
        }
      },
      fail: reject
    });
  });
}

module.exports = {
  upload,
  chooseAndUpload
};
