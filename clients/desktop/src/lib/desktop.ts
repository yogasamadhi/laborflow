import { isTauri } from "@tauri-apps/api/core";
import { save } from "@tauri-apps/plugin-dialog";
import { writeFile } from "@tauri-apps/plugin-fs";
import { openUrl } from "@tauri-apps/plugin-opener";

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
  if (!isTauri()) {
    downloadInBrowser(blob, filename);
    return true;
  }
  const path = await save({ defaultPath: filename });
  if (!path) return false;
  await writeFile(path, new Uint8Array(await blob.arrayBuffer()));
  return true;
}

export async function openExternalUrl(url: string) {
  const target = new URL(url);
  if (target.protocol !== "https:" && target.protocol !== "http:") {
    throw new Error("仅允许打开 HTTP 或 HTTPS 地址");
  }
  if (isTauri()) {
    await openUrl(target.toString());
    return;
  }
  window.open(target.toString(), "_blank", "noopener,noreferrer");
}
