import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

const webPort = Number(process.env.WEB_PORT ?? 5175);
const apiPort = Number(process.env.SERVER_PORT ?? 8089);

export default defineConfig({
  plugins: [react()],
  server: {
    port: webPort,
    strictPort: true,
    proxy: {
      "/api": `http://localhost:${apiPort}`
    }
  }
});
