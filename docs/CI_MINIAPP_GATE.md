# Miniapp Local Quality Gate

## Scope

The active Taro client in `clients/miniapp` uses Bun 1.3.14 for installation,
script execution, TypeScript, Vite, and both WeChat and H5 builds. Its
`bunfig.toml` forces packages with Node shebangs to run under Bun, so the gate
does not require a system Node.js installation.

Run from the repository root:

```bash
cd clients/miniapp
bun install --frozen-lockfile
bun pm untrusted
bun run check
bun run build:h5
```

`check` runs the TypeScript project build and the WeChat production build. The
H5 command validates the second supported Vite target. `bun pm untrusted` must
report no blocked lifecycle scripts.

## Manual Evidence

WeChat DevTools rendering, device permissions, real uploads, account binding,
and real network behavior remain manual smoke responsibilities. Every
user-visible miniapp change must attach a completed record based on
[`MINIAPP_SMOKE_EVIDENCE_TEMPLATE.md`](MINIAPP_SMOKE_EVIDENCE_TEMPLATE.md), with
the identity/session flow covered by
[`MINIAPP_IDENTITY_CONTRACT_QA_CHECKLIST.md`](MINIAPP_IDENTITY_CONTRACT_QA_CHECKLIST.md).

The native client in `legacy/miniapp` is retained only as a migration reference
and keeps its npm/Node checks until that directory is reworked.
