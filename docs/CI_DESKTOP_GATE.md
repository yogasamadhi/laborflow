# Desktop Local Quality Gate

## Scope

The desktop client uses Bun 1.3.14 for dependency installation, Electron Forge,
native-module rebuild orchestration, linting, type checking, testing, and
renderer builds. The launched Electron application uses Electron's embedded
Node runtime; no separately installed system Node.js is required.

Run from the repository root:

```bash
cd clients/desktop
bun install --frozen-lockfile
bun pm untrusted
bun run lint
bun run typecheck
bun run test
bun run build:renderer
```

`bun pm untrusted` must report no blocked lifecycle scripts. The renderer gate
covers the renderer, Electron main process, and preload bridge. `bunfig.toml`
forces Electron Forge, ESLint, TypeScript, Vitest, Prettier, and Vite to execute
under Bun. Forge does not recognize Bun as a package-manager name, so its three
entry scripts provide a compatibility user-agent value; this avoids invoking
an external npm/Node installation and does not change dependency installation.

## Platform Build

Release candidates require a build on every target operating system:

```bash
cd clients/desktop
bun run package
bun run build
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
