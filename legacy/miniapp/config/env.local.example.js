/**
 * Example local/release environment override for the miniapp.
 *
 * Copy this file to config/env.local.js (gitignored) and edit the host.
 *
 * - For local development pointing at your machine's backend: you usually do
 *   NOT need this file — the built-in dev default (http://localhost:8089) is
 *   used automatically.
 * - For staging/production: set both URLs to your real https host, e.g.
 *     https://api.laborflow.example.com/api
 *
 * Production URLs MUST be https and MUST be added to the WeChat 合法域名 list
 * in the miniapp admin console before submission.
 */
module.exports = {
  baseURL: "https://api.laborflow.example.com/api",
  uploadURL: "https://api.laborflow.example.com/api/files/upload"
};
