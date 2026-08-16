import { writeFile } from "node:fs/promises";
import path from "node:path";
import { app, BrowserWindow, dialog, ipcMain, net, session, shell, type IpcMainInvokeEvent } from "electron";
import started from "electron-squirrel-startup";
import {
  desktopChannels,
  type DesktopHttpBody,
  type DesktopHttpRequest,
  type DesktopHttpResponse,
  type DesktopSaveFileRequest
} from "./contracts";

declare const MAIN_WINDOW_VITE_DEV_SERVER_URL: string;
declare const MAIN_WINDOW_VITE_NAME: string;

const apiBaseUrl = new URL(
  (import.meta.env.VITE_API_BASE_URL?.trim() || "http://localhost:8089/api").replace(/\/+$/, "")
);
const allowedMethods = new Set(["GET", "POST", "PUT", "PATCH", "DELETE"]);
const blockedHeaders = new Set(["connection", "content-length", "cookie", "host", "origin", "referer"]);

function isHttpUrl(value: string) {
  try {
    const url = new URL(value);
    return (url.protocol === "http:" || url.protocol === "https:") && !url.username && !url.password;
  } catch {
    return false;
  }
}

function assertApiUrl(value: string) {
  const target = new URL(value);
  const basePath = apiBaseUrl.pathname.replace(/\/+$/, "");
  const matchesPath = target.pathname === basePath || target.pathname.startsWith(`${basePath}/`);
  if (target.origin !== apiBaseUrl.origin || !matchesPath || target.username || target.password) {
    throw new Error("请求地址不在已配置的 API 范围内");
  }
  if (
    target.protocol !== "https:" &&
    !(target.protocol === "http:" && ["localhost", "127.0.0.1"].includes(target.hostname))
  ) {
    throw new Error("非本地 API 必须使用 HTTPS");
  }
  return target.toString();
}

function copyArrayBuffer(value: Uint8Array) {
  const copy = new Uint8Array(value.byteLength);
  copy.set(value);
  return copy.buffer;
}

function createRequestBody(body?: DesktopHttpBody): BodyInit | undefined {
  if (!body) return undefined;
  if (body.type === "text") return body.value;
  if (body.type === "bytes") return copyArrayBuffer(body.value);
  const formData = new FormData();
  for (const entry of body.entries) {
    if (entry.type === "text") {
      formData.append(entry.name, entry.value);
    } else {
      const blob = new Blob([copyArrayBuffer(entry.value)], { type: entry.contentType || "application/octet-stream" });
      formData.append(entry.name, blob, path.basename(entry.filename));
    }
  }
  return formData;
}

function normalizeRequestHeaders(headers: Record<string, string>, multipart: boolean) {
  const normalized = new Headers();
  for (const [name, value] of Object.entries(headers)) {
    const lowerName = name.toLowerCase();
    if (
      blockedHeaders.has(lowerName) ||
      lowerName.startsWith("proxy-") ||
      lowerName.startsWith("sec-") ||
      (multipart && lowerName === "content-type")
    ) {
      continue;
    }
    normalized.set(name, value);
  }
  return normalized;
}

async function performHttpRequest(request: DesktopHttpRequest): Promise<DesktopHttpResponse> {
  const method = request.method.toUpperCase();
  if (!allowedMethods.has(method)) throw new Error("不支持的 HTTP 请求方法");
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), Math.min(Math.max(request.timeoutMs, 1), 120_000));
  try {
    const response = await net.fetch(assertApiUrl(request.url), {
      method,
      headers: normalizeRequestHeaders(request.headers, request.body?.type === "form-data"),
      body: method === "GET" ? undefined : createRequestBody(request.body),
      credentials: "omit",
      redirect: "error",
      signal: controller.signal
    });
    const headers: Record<string, string> = {};
    response.headers.forEach((value, name) => {
      headers[name] = value;
    });
    return {
      status: response.status,
      statusText: response.statusText,
      headers,
      body: new Uint8Array(await response.arrayBuffer())
    };
  } finally {
    clearTimeout(timeout);
  }
}

async function saveFile(event: IpcMainInvokeEvent, request: DesktopSaveFileRequest) {
  if (!request?.filename || !(request.data instanceof Uint8Array)) throw new Error("保存文件参数无效");
  const filename = path.basename(request.filename.trim()).slice(0, 255) || "download";
  const options = { title: "保存文件", defaultPath: filename };
  const owner = BrowserWindow.fromWebContents(event.sender);
  const result = owner ? await dialog.showSaveDialog(owner, options) : await dialog.showSaveDialog(options);
  if (result.canceled || !result.filePath) return false;
  await writeFile(result.filePath, request.data);
  return true;
}

function registerDesktopHandlers() {
  ipcMain.handle(desktopChannels.saveFile, saveFile);
  ipcMain.handle(desktopChannels.openExternal, async (_event, value: string) => {
    if (!isHttpUrl(value)) throw new Error("仅允许打开 HTTP 或 HTTPS 地址");
    await shell.openExternal(new URL(value).toString());
  });
  ipcMain.handle(desktopChannels.request, (_event, request: DesktopHttpRequest) => performHttpRequest(request));
}

function openExternalFromWindow(value: string) {
  if (isHttpUrl(value)) void shell.openExternal(new URL(value).toString()).catch(() => undefined);
}

function createWindow() {
  const mainWindow = new BrowserWindow({
    title: "LaborFlow",
    width: 1440,
    height: 900,
    minWidth: 1100,
    minHeight: 720,
    backgroundColor: "#f4f7fb",
    show: false,
    icon: path.join(app.getAppPath(), "resources", "icons", "icon.png"),
    webPreferences: {
      preload: path.join(__dirname, "preload.js"),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true,
      webSecurity: true
    }
  });

  mainWindow.once("ready-to-show", () => {
    if (process.platform === "darwin") app.focus({ steal: true });
    mainWindow.show();
    mainWindow.focus();
    process.stdout.write("LaborFlow desktop window is ready\n");
  });
  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    openExternalFromWindow(url);
    return { action: "deny" };
  });
  mainWindow.webContents.on("will-navigate", (event, url) => {
    if (url === mainWindow.webContents.getURL()) return;
    event.preventDefault();
    openExternalFromWindow(url);
  });

  if (MAIN_WINDOW_VITE_DEV_SERVER_URL) {
    void mainWindow.loadURL(MAIN_WINDOW_VITE_DEV_SERVER_URL);
  } else {
    void mainWindow.loadFile(path.join(__dirname, `../renderer/${MAIN_WINDOW_VITE_NAME}/index.html`));
  }
}

if (started) {
  app.quit();
} else {
  app.setAppUserModelId("com.company.laborflow");
  registerDesktopHandlers();
  void app.whenReady().then(() => {
    session.defaultSession.setPermissionRequestHandler((_webContents, _permission, callback) => callback(false));
    createWindow();
    app.on("activate", () => {
      if (BrowserWindow.getAllWindows().length === 0) createWindow();
    });
  });
  app.on("window-all-closed", () => {
    if (process.platform !== "darwin") app.quit();
  });
}
