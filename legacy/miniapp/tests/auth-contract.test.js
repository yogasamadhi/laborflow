const assert = require("node:assert/strict");
const path = require("node:path");
const test = require("node:test");

const MINIAPP_ROOT = path.resolve(__dirname, "..");
const MODULES = [
  "utils/auth.js",
  "utils/request.js",
  "services/auth.js",
  "services/file.js",
  "services/project.js"
].map((file) => path.join(MINIAPP_ROOT, file));
const LOGIN_PAGE = path.join(MINIAPP_ROOT, "pages/login/index.js");

function createRuntime() {
  const storage = new Map();
  const requests = [];
  const uploads = [];
  const relaunches = [];
  let requestHandler = null;
  let uploadHandler = null;

  global.wx = {
    getStorageSync(key) {
      return storage.get(key);
    },
    setStorageSync(key, value) {
      storage.set(key, value);
    },
    removeStorageSync(key) {
      storage.delete(key);
    },
    request(options) {
      requests.push(options);
      requestHandler(options);
    },
    uploadFile(options) {
      uploads.push(options);
      return uploadHandler(options);
    },
    reLaunch(options) {
      relaunches.push(options.url);
      if (options.complete) options.complete();
    }
  };

  return {
    storage,
    requests,
    uploads,
    relaunches,
    setRequestHandler(handler) {
      requestHandler = handler;
    },
    setUploadHandler(handler) {
      uploadHandler = handler;
    }
  };
}

function loadModules() {
  MODULES.forEach((modulePath) => delete require.cache[modulePath]);
  return {
    auth: require(path.join(MINIAPP_ROOT, "utils/auth.js")),
    request: require(path.join(MINIAPP_ROOT, "utils/request.js")),
    authService: require(path.join(MINIAPP_ROOT, "services/auth.js")),
    fileService: require(path.join(MINIAPP_ROOT, "services/file.js")),
    projectService: require(path.join(MINIAPP_ROOT, "services/project.js"))
  };
}

function loadLoginPage() {
  let definition;
  const originalPage = global.Page;
  global.Page = (page) => {
    definition = page;
  };
  delete require.cache[LOGIN_PAGE];
  require(LOGIN_PAGE);
  global.Page = originalPage;
  return definition;
}

function createPageInstance(definition) {
  const instance = Object.assign({}, definition, {
    data: Object.assign({}, definition.data)
  });
  instance.setData = (patch) => {
    Object.assign(instance.data, patch);
  };
  return instance;
}

function loginPayload(overrides) {
  return Object.assign({
    token: "token-123",
    userId: "1900000000000000001",
    employeeId: "1900000000000000002",
    username: "field-user",
    nickname: "现场人员",
    roles: ["EMPLOYEE"]
  }, overrides || {});
}

test("session cache accepts a complete login response and clears malformed cache", () => {
  const runtime = createRuntime();
  const { auth } = loadModules();

  const user = auth.setLogin(loginPayload({ roles: ["EMPLOYEE", "", 3] }));
  assert.deepEqual(user, {
    userId: "1900000000000000001",
    employeeId: "1900000000000000002",
    username: "field-user",
    nickname: "现场人员",
    roles: ["EMPLOYEE"]
  });
  assert.equal(auth.getToken(), "token-123");
  assert.equal(auth.getSession().user.username, "field-user");
  assert.throws(() => auth.setLogin(loginPayload({ token: "" })), /登录信息不完整/);
  assert.equal(auth.getSession(), null);

  auth.setLogin(loginPayload());
  const refreshedUser = auth.refreshUser({
    userId: "1900000000000000001",
    employeeId: null,
    username: "field-user",
    nickname: "更新后的昵称",
    roles: ["EMPLOYEE"]
  });
  assert.equal(auth.getToken(), "token-123");
  assert.equal(refreshedUser.nickname, "更新后的昵称");

  runtime.storage.set("laborflow_user", { userId: "1900000000000000001", username: "", roles: [] });
  assert.equal(auth.getSession(), null);
  assert.equal(runtime.storage.has("laborflow_token"), false);
  assert.equal(runtime.storage.has("laborflow_user"), false);
});

test("session IDs are normalized to decimal strings without losing snowflake precision", () => {
  createRuntime();
  const { auth } = loadModules();

  const numericUser = auth.setLogin(loginPayload({ userId: 9, employeeId: 18 }));
  assert.equal(numericUser.userId, "9");
  assert.equal(numericUser.employeeId, "18");

  const snowflakeUser = auth.setLogin(loginPayload({
    userId: "9223372036854775807",
    employeeId: "1900000000000000002"
  }));
  assert.equal(snowflakeUser.userId, "9223372036854775807");
  assert.equal(snowflakeUser.employeeId, "1900000000000000002");

  for (const userId of [0, -1, Number.MAX_SAFE_INTEGER + 1, "0", "-1", "9.5", "not-an-id"]) {
    assert.throws(() => auth.setLogin(loginPayload({ userId })), /登录信息不完整/);
  }
});

test("expired sessions clear cache and forbidden responses preserve it", async () => {
  const runtime = createRuntime();
  const { auth, request } = loadModules();
  auth.setLogin(loginPayload());

  runtime.setRequestHandler((options) => options.success({
    statusCode: 401,
    data: { code: 401, message: "令牌过期" }
  }));
  await assert.rejects(request.get("/projects"), /登录已失效/);
  assert.equal(auth.getSession(), null);
  assert.deepEqual(runtime.relaunches, ["/pages/login/index"]);

  auth.setLogin(loginPayload());
  runtime.setRequestHandler((options) => options.success({
    statusCode: 403,
    data: { code: 403, message: "无项目访问权限" }
  }));
  await assert.rejects(request.get("/projects"), /无项目访问权限/);
  assert.equal(auth.getToken(), "token-123");
  assert.deepEqual(runtime.relaunches, ["/pages/login/index"]);
});

test("auth service keeps login errors in place and uses the checked-in API contract", async () => {
  const runtime = createRuntime();
  const { auth, authService } = loadModules();
  auth.setLogin(loginPayload());
  runtime.setRequestHandler((options) => options.success({
    statusCode: 401,
    data: { code: 401, message: "账号或密码错误" }
  }));
  await assert.rejects(authService.login({ username: "bad", password: "bad" }), /账号或密码错误/);
  assert.equal(auth.getToken(), "token-123");
  assert.deepEqual(runtime.relaunches, []);

  auth.clearLogin();
  runtime.setRequestHandler((options) => options.success({
    statusCode: 200,
    data: { code: 0, data: options.url.endsWith("/auth/wechat/session") ? { openid: "openid-1", bound: false } : loginPayload() }
  }));
  const wechatSession = await authService.wechatSession("wx-code-1");
  const login = await authService.login({
    username: "field-user",
    password: "password",
    wechatOpenid: "openid-1",
    wechatNickname: "现场人员"
  });
  auth.setLogin(login);
  await authService.me();
  await authService.logout();

  assert.deepEqual(wechatSession, { openid: "openid-1", bound: false });
  assert.deepEqual(runtime.requests.map((item) => ({
    path: item.url.replace(/^.*\/api/, ""),
    method: item.method,
    data: item.data,
    authorization: item.header.Authorization
  })), [
    { path: "/auth/login", method: "POST", data: { username: "bad", password: "bad" }, authorization: undefined },
    { path: "/auth/wechat/session", method: "POST", data: { code: "wx-code-1" }, authorization: undefined },
    {
      path: "/auth/login",
      method: "POST",
      data: {
        username: "field-user",
        password: "password",
        wechatOpenid: "openid-1",
        wechatNickname: "现场人员"
      },
      authorization: undefined
    },
    { path: "/auth/me", method: "GET", data: {}, authorization: "Bearer token-123" },
    { path: "/auth/logout", method: "POST", data: {}, authorization: "Bearer token-123" }
  ]);
});

test("project attachment services use server-supported file types", async () => {
  const runtime = createRuntime();
  const { projectService } = loadModules();
  runtime.setRequestHandler((options) => options.success({ statusCode: 200, data: { code: 0, data: null } }));

  await projectService.attachDailyReportFile(12, 44);
  await projectService.attachVariationFile(13, 45);

  assert.deepEqual(runtime.requests.map((item) => item.data), [
    { fileId: 44, fileType: "photo" },
    { fileId: 45, fileType: "photo" }
  ]);
});

test("file upload clears an expired session but preserves forbidden-session state", async () => {
  const runtime = createRuntime();
  const { auth, fileService } = loadModules();
  auth.setLogin(loginPayload());
  runtime.setUploadHandler((options) => options.success({
    statusCode: 401,
    data: JSON.stringify({ code: 401, message: "令牌过期" })
  }));
  await assert.rejects(fileService.upload("/tmp/photo.jpg"), /登录已失效/);
  assert.equal(auth.getSession(), null);
  assert.deepEqual(runtime.relaunches, ["/pages/login/index"]);

  auth.setLogin(loginPayload());
  runtime.setUploadHandler((options) => options.success({
    statusCode: 403,
    data: JSON.stringify({ code: 403, message: "无上传权限" })
  }));
  await assert.rejects(fileService.upload("/tmp/photo.jpg"), /无上传权限/);
  assert.equal(auth.getToken(), "token-123");
});

test("file selection exposes successfully uploaded files when a later upload fails", async () => {
  const runtime = createRuntime();
  const { auth, fileService } = loadModules();
  const progress = [];
  auth.setLogin(loginPayload());
  global.wx.chooseMedia = (options) => {
    assert.equal(options.count, 2);
    options.success({ tempFiles: [{ tempFilePath: "/tmp/one.jpg" }, { tempFilePath: "/tmp/two.jpg" }] });
  };
  runtime.setUploadHandler((options) => {
    if (runtime.uploads.length === 1) {
      options.success({ statusCode: 200, data: JSON.stringify({ code: 0, data: { id: 51 } }) });
      return {
        onProgressUpdate(listener) {
          listener({ progress: 70 });
        }
      };
    }
    options.fail({ errMsg: "request:fail timeout" });
    return { onProgressUpdate() {} };
  });

  await assert.rejects(fileService.chooseAndUpload({ maxCount: 2, onProgress: (event) => progress.push(event) }), (error) => {
    assert.deepEqual(error.uploadedFiles, [{ id: 51 }]);
    return true;
  });
  assert.deepEqual(progress, [
    { current: 1, total: 2, progress: 70 },
    { current: 1, total: 2, progress: 100 }
  ]);
});

test("login initialization absorbs background session failures and reports explicit wx.login failures", async () => {
  createRuntime();
  const page = createPageInstance(loadLoginPage());
  const toasts = [];
  global.wx.login = () => {
    throw new Error("wx.login unavailable");
  };
  global.wx.showToast = (options) => toasts.push(options);

  await assert.rejects(page.refreshWechatSession(true), /wx\.login unavailable/);
  assert.deepEqual(toasts, [{ title: "wx.login unavailable", icon: "none" }]);

  page.refreshWechatSession = () => Promise.reject(new Error("background session unavailable"));
  page.onLoad();
  await new Promise((resolve) => setImmediate(resolve));
});
