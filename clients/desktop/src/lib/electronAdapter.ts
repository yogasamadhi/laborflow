import axios, {
  AxiosError,
  AxiosHeaders,
  CanceledError,
  type AxiosAdapter,
  type AxiosResponse,
  type ResponseType
} from "axios";
import type { DesktopHttpBody, DesktopHttpResponse } from "../../electron/contracts";
import { getDesktopBridge } from "./desktop";

function copyBytes(value: ArrayBufferView | ArrayBuffer) {
  if (value instanceof ArrayBuffer) return new Uint8Array(value.slice(0));
  return Uint8Array.from(new Uint8Array(value.buffer, value.byteOffset, value.byteLength));
}

export async function serializeDesktopBody(data: unknown): Promise<DesktopHttpBody | undefined> {
  if (data === undefined || data === null) return undefined;
  if (typeof data === "string") return { type: "text", value: data };
  if (typeof FormData !== "undefined" && data instanceof FormData) {
    const entries: Extract<DesktopHttpBody, { type: "form-data" }>["entries"] = [];
    for (const [name, value] of data.entries()) {
      if (typeof value === "string") {
        entries.push({ type: "text", name, value });
      } else {
        entries.push({
          type: "file",
          name,
          filename: value.name || "file",
          contentType: value.type,
          value: new Uint8Array(await value.arrayBuffer())
        });
      }
    }
    return { type: "form-data", entries };
  }
  if (typeof Blob !== "undefined" && data instanceof Blob) {
    return { type: "bytes", value: new Uint8Array(await data.arrayBuffer()) };
  }
  if (data instanceof ArrayBuffer || ArrayBuffer.isView(data)) {
    return { type: "bytes", value: copyBytes(data) };
  }
  if (data instanceof URLSearchParams) return { type: "text", value: data.toString() };
  return { type: "text", value: JSON.stringify(data) };
}

function normalizeHeaders(headers: AxiosHeaders) {
  const result: Record<string, string> = {};
  const values = headers.normalize(true).toJSON(true);
  for (const [name, value] of Object.entries(values)) {
    if (value === null || value === undefined) continue;
    result[name] = Array.isArray(value) ? value.join(", ") : String(value);
  }
  return result;
}

export function decodeDesktopResponse(response: DesktopHttpResponse, responseType?: ResponseType) {
  const bytes = Uint8Array.from(response.body);
  if (responseType === "arraybuffer") return bytes.buffer;
  if (responseType === "blob") {
    return new Blob([bytes.buffer], { type: response.headers["content-type"] || "application/octet-stream" });
  }
  if (responseType === "stream") throw new AxiosError("Electron 桌面端不支持流式响应");
  return new TextDecoder().decode(bytes);
}

export const electronAdapter: AxiosAdapter = async config => {
  const desktopBridge = getDesktopBridge();
  if (!desktopBridge) throw new AxiosError("Electron 桌面桥接不可用", AxiosError.ERR_NOT_SUPPORT, config);
  if (config.signal?.aborted) throw new CanceledError(undefined, config);

  try {
    const result = await desktopBridge.request({
      url: axios.getUri(config),
      method: (config.method || "GET").toUpperCase(),
      headers: normalizeHeaders(AxiosHeaders.from(config.headers)),
      body: await serializeDesktopBody(config.data),
      timeoutMs: config.timeout || 15_000
    });
    if (config.signal?.aborted) throw new CanceledError(undefined, config);
    const response: AxiosResponse = {
      data: decodeDesktopResponse(result, config.responseType),
      status: result.status,
      statusText: result.statusText,
      headers: AxiosHeaders.from(result.headers),
      config
    };
    if (!config.validateStatus || config.validateStatus(result.status)) return response;
    const code = result.status >= 500 ? AxiosError.ERR_BAD_RESPONSE : AxiosError.ERR_BAD_REQUEST;
    throw new AxiosError(`Request failed with status code ${result.status}`, code, config, undefined, response);
  } catch (error) {
    if (error instanceof AxiosError) throw error;
    const cause = error instanceof Error ? error : new Error("Electron HTTP 请求失败");
    throw AxiosError.from(cause, AxiosError.ERR_NETWORK, config);
  }
};
