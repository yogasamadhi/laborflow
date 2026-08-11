/**
 * Lightweight scoped logger for the miniapp.
 *
 * Previously the miniapp had zero console.* calls, and many network failure
 * paths silently returned []/null — a full backend outage rendered the home
 * page blank with no signal to the user or any log. This logger centralises
 * warning output so failures are at least visible in the WeChat developer
 * tools console and in real-device vConsole.
 *
 * Keep calls cheap: in production builds the WeChat toolchain does not strip
 * console calls, so we guard on an explicit flag rather than process.env.
 */

// Set to false only if you want to silence diagnostics in a release build.
const ENABLED = true;

function warn(scope, detail) {
  if (!ENABLED) return;
  // Coerce error objects to a readable message; avoid dumping huge payloads.
  const message = detail instanceof Error
    ? detail.message
    : typeof detail === "string" ? detail : safeStringify(detail);
  // eslint-disable-next-line no-console
  console.warn("[laborflow:" + scope + "]", message);
}

function error(scope, detail) {
  if (!ENABLED) return;
  const message = detail instanceof Error
    ? (detail.stack || detail.message)
    : typeof detail === "string" ? detail : safeStringify(detail);
  // eslint-disable-next-line no-console
  console.error("[laborflow:" + scope + "]", message);
}

function safeStringify(value) {
  try {
    return JSON.stringify(value);
  } catch (_) {
    return String(value);
  }
}

module.exports = { warn, error };
