const env = require("../config/env");
const auth = require("../utils/auth");

function upload(path) {
  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url: env.uploadURL,
      filePath: path,
      name: "file",
      header: {
        Authorization: "Bearer " + auth.getToken()
      },
      success(res) {
        let body = {};
        try {
          body = JSON.parse(res.data);
        } catch (error) {
          reject(new Error("上传结果解析失败"));
          return;
        }
        if (res.statusCode >= 200 && res.statusCode < 300 && body.code === 0) {
          resolve(body.data);
          return;
        }
        reject(new Error(body.message || "上传失败"));
      },
      fail(err) {
        reject(new Error(err.errMsg || "上传失败"));
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
