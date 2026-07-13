const env = require("../config/env");
const auth = require("../utils/auth");
const request = require("../utils/request");

function upload(path, options) {
  const settings = options || {};
  return new Promise((resolve, reject) => {
    const token = auth.getToken();
    const header = {};
    if (token) header.Authorization = "Bearer " + token;
    const task = wx.uploadFile({
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
        const message = (err && err.errMsg) || "";
        reject(new Error(/cancel/i.test(message) ? "已取消上传" : "上传失败，请检查网络后重试"));
      }
    });
    if (settings.onProgress && task && typeof task.onProgressUpdate === "function") {
      task.onProgressUpdate((event) => settings.onProgress(event && event.progress ? event.progress : 0));
    }
  });
}

function chooseAndUpload(options) {
  const settings = options || {};
  const requestedCount = Number(settings.maxCount);
  const maxCount = Number.isFinite(requestedCount) && requestedCount > 0
    ? Math.min(6, Math.max(1, Math.floor(requestedCount)))
    : 6;
  return new Promise((resolve, reject) => {
    wx.chooseMedia({
      count: maxCount,
      mediaType: ["image"],
      sourceType: ["album", "camera"],
      success: async (res) => {
        const files = [];
        try {
          const tempFiles = res.tempFiles || [];
          for (let index = 0; index < tempFiles.length; index += 1) {
            const item = tempFiles[index];
            const uploaded = await upload(item.tempFilePath, {
              onProgress(progress) {
                if (settings.onProgress) {
                  settings.onProgress({ current: index + 1, total: tempFiles.length, progress });
                }
              }
            });
            files.push(uploaded);
            if (settings.onProgress) {
              settings.onProgress({ current: index + 1, total: tempFiles.length, progress: 100 });
            }
          }
          resolve(files);
        } catch (error) {
          error.uploadedFiles = files;
          reject(error);
        }
      },
      fail(err) {
        const message = (err && err.errMsg) || "";
        reject(new Error(/cancel/i.test(message) ? "已取消选择照片" : "选择照片失败，请稍后重试"));
      }
    });
  });
}

module.exports = {
  upload,
  chooseAndUpload
};
