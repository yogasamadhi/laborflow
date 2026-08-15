interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

interface Window {
  readonly laborflowDesktop?: import("../electron/contracts").LaborFlowDesktopApi;
}
