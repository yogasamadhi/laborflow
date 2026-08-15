import type { LaborFlowDesktopApi } from "../../electron/contracts";

export function getDesktopBridge(): LaborFlowDesktopApi | undefined {
  return typeof window === "undefined" ? undefined : window.laborflowDesktop;
}

export function isElectronRuntime() {
  return getDesktopBridge()?.isElectron === true;
}

function downloadInBrowser(blob: Blob, filename: string) {
  const objectUrl = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = objectUrl;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(objectUrl);
}

export async function saveBlob(blob: Blob, filename: string) {
  const desktopBridge = getDesktopBridge();
  if (!desktopBridge) {
    downloadInBrowser(blob, filename);
    return true;
  }
  return desktopBridge.saveFile({
    filename,
    data: new Uint8Array(await blob.arrayBuffer())
  });
}

export async function openExternalUrl(url: string) {
  const target = new URL(url);
  if (target.protocol !== "https:" && target.protocol !== "http:") {
    throw new Error("仅允许打开 HTTP 或 HTTPS 地址");
  }
  const desktopBridge = getDesktopBridge();
  if (desktopBridge) {
    await desktopBridge.openExternal(target.toString());
    return;
  }
  window.open(target.toString(), "_blank", "noopener,noreferrer");
}
