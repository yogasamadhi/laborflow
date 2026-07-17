/**
 * Environment configuration for the LaborFlow miniapp.
 *
 * WeChat miniapps have no process.env at runtime, so environment switching is
 * done by file rather than by environment variable. The active environment is
 * selected in priority order:
 *
 *   1. config/env.local.js   — gitignored, machine/release specific.
 *                              Copy config/env.local.example.js and edit it.
 *   2. HARD-CODED fallback   — development defaults pointing at the local
 *                              backend (http://localhost:8089). Used when no
 *                              local override exists, e.g. a fresh clone.
 *
 * PRODUCTION releases MUST ship a config/env.local.js with the real https host
 * before building. See docs/MINIAPP_RELEASE_CHECKLIST.md.
 *
 * The WeChat developer tools also require urlCheck to be disabled for the
 * localhost dev host (project.config.json) and re-enabled + the production
 * https host added to the 合法域名 list before submission.
 */

const developmentDefaults = {
  baseURL: "http://localhost:8089/api",
  uploadURL: "http://localhost:8089/api/files/upload"
};

function resolve() {
  // env.local.js is gitignored; require lazily so the build never breaks when
  // it is absent (fresh clone / CI syntax check).
  try {
    // require resolves relative to this file.
    const local = require("./env.local.js");
    if (local && local.baseURL && local.uploadURL) {
      return local;
    }
    // Partial override is not allowed — a misconfigured half-host is worse than
    // the dev default. Warn loudly.
    // eslint-disable-next-line no-console
    console.warn("[env] config/env.local.js is missing baseURL/uploadURL; falling back to development defaults.");
  } catch (error) {
    // Module not found is the expected path in dev; anything else is worth a hint.
    if (!String(error && error.message || "").includes("Cannot find module")) {
      // eslint-disable-next-line no-console
      console.warn("[env] could not load config/env.local.js:", error && error.message);
    }
  }
  return developmentDefaults;
}

module.exports = resolve();
