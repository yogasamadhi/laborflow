# Desktop CI Gate

## Scope

`.workflow/PRDesktopPipeline.yml` defines the Gitee Go renderer quality job for
pull requests targeting `main`. It uses the pnpm version pinned in
`clients/desktop/package.json` and runs:

```bash
pnpm --dir clients/desktop install --frozen-lockfile
pnpm --dir clients/desktop lint
pnpm --dir clients/desktop typecheck
pnpm --dir clients/desktop test
pnpm --dir clients/desktop build:renderer
```

The typecheck covers the renderer, Electron main process, and preload bridge; the
renderer gate remains operating-system independent. A release must additionally
run `pnpm --dir clients/desktop build` on every target operating system because
Electron Forge installers and code signing are platform-specific. Windows, macOS, and
Linux release builds are not interchangeable.

## Local verification

Run the renderer quality sequence before the platform build:

```bash
pnpm --dir clients/desktop lint
pnpm --dir clients/desktop typecheck
pnpm --dir clients/desktop test
pnpm --dir clients/desktop build:renderer
pnpm --dir clients/desktop build
```

Production builds must set `VITE_API_BASE_URL` to the HTTPS Spring API origin.
The committed default is only for the local server on port `8089`. Signing keys,
certificates, passwords, and updater private keys must only be provided through
the protected release environment.

Electron Forge 7 requires pnpm's hoisted linker. The desktop workspace also sets
`blockExoticSubdeps: false` because Forge 7.11.2 depends on `@electron/rebuild`
3.7.2, which resolves `@electron/node-gyp` from its upstream Git repository.
Keep this exception scoped to `clients/desktop` and remove it after Forge adopts
the newer rebuild dependency.

## Contract

- Any renderer lint, typecheck, test, or build failure blocks merge after the
  Gitee Go status is configured as required.
- Electron renderer processes must keep Node.js integration disabled, context
  isolation and sandboxing enabled, and expose only the minimum API-scoped HTTP,
  save-dialog, file-write, and external-URL operations through the preload bridge.
- Release candidates require a successful signed platform build and a manual
  launch smoke check on each distributed operating system.
