import { contextBridge, ipcRenderer } from "electron";
import { desktopChannels, type LaborFlowDesktopApi } from "./contracts";

const desktopApi: LaborFlowDesktopApi = {
  isElectron: true,
  saveFile: request => ipcRenderer.invoke(desktopChannels.saveFile, request),
  openExternal: url => ipcRenderer.invoke(desktopChannels.openExternal, url),
  request: request => ipcRenderer.invoke(desktopChannels.request, request)
};

contextBridge.exposeInMainWorld("laborflowDesktop", Object.freeze(desktopApi));
