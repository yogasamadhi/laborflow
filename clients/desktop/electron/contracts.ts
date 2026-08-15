export const desktopChannels = {
  saveFile: "laborflow:save-file",
  openExternal: "laborflow:open-external",
  request: "laborflow:http-request"
} as const;

export type DesktopHttpBody =
  | { type: "text"; value: string }
  | { type: "bytes"; value: Uint8Array }
  | {
      type: "form-data";
      entries: Array<
        | { type: "text"; name: string; value: string }
        | { type: "file"; name: string; filename: string; contentType: string; value: Uint8Array }
      >;
    };

export type DesktopHttpRequest = {
  url: string;
  method: string;
  headers: Record<string, string>;
  body?: DesktopHttpBody;
  timeoutMs: number;
};

export type DesktopHttpResponse = {
  status: number;
  statusText: string;
  headers: Record<string, string>;
  body: Uint8Array;
};

export type DesktopSaveFileRequest = {
  filename: string;
  data: Uint8Array;
};

export type LaborFlowDesktopApi = {
  isElectron: true;
  saveFile(request: DesktopSaveFileRequest): Promise<boolean>;
  openExternal(url: string): Promise<void>;
  request(request: DesktopHttpRequest): Promise<DesktopHttpResponse>;
};
