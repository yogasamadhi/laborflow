# Desktop Local Quality Gate

## Scope

The desktop client uses pnpm 11.22.0 for dependency installation, Electron Forge,
native-module rebuild orchestration, linting, type checking, testing, and
renderer builds. The launched Electron application uses Electron's embedded
Node runtime; no separately installed system Node.js is required.

Run from the repository root:

```bash
cd clients/desktop
pnpm install --frozen-lockfile
pnpm ignored-builds
pnpm run lint
pnpm run typecheck
pnpm run test
pnpm run build:renderer
```

`pnpm ignored-builds` must report no blocked lifecycle scripts. The renderer
gate covers the renderer, Electron main process, and preload bridge.
`pnpm-workspace.yaml` pins the hoisted node linker, the approved build-script
allowlist, and the `@electron/rebuild` override; Electron Forge resolves pnpm
natively, so no package-manager user-agent override is required.

## Platform Build

Release candidates require a build on every target operating system:

```bash
cd clients/desktop
pnpm run package
pnpm run build
```

Electron Forge installers and signing are platform-specific. Windows, macOS,
and Linux outputs are not interchangeable. Production builds must set
`VITE_API_BASE_URL` to the HTTPS Spring API origin; the committed default is
only for local development.

## Contract

- Any install, trust, lint, typecheck, test, or renderer-build failure fails the local gate.
- Electron renderer processes keep Node integration disabled, context isolation and sandboxing enabled.
- The preload bridge exposes only the minimum API, save-dialog, file-write, and external-URL operations.
- A release requires a successful signed platform build and a manual launch smoke check.
