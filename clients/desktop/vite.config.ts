import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

const desktopPort = 5175;
const tauriHost = process.env.TAURI_DEV_HOST;

export default defineConfig({
  plugins: [react()],
  clearScreen: false,
  server: {
    host: tauriHost || false,
    port: desktopPort,
    strictPort: true,
    hmr: tauriHost
      ? {
          protocol: "ws",
          host: tauriHost,
          port: 5176
        }
      : undefined,
    watch: {
      ignored: ["**/src-tauri/**"]
    }
  }
});
