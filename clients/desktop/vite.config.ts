import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

const desktopPort = 5175;

export default defineConfig({
  base: "./",
  plugins: [react()],
  clearScreen: false,
  server: {
    host: false,
    port: desktopPort,
    strictPort: true,
    watch: {
      ignored: ["**/electron/**"]
    }
  }
});
