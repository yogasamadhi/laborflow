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

The renderer gate is operating-system independent. A release must additionally
run `pnpm --dir clients/desktop build` on every target operating system because
Tauri installers and code signing are platform-specific. Windows, macOS, and
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

## Contract

- Any renderer lint, typecheck, test, or build failure blocks merge after the
  Gitee Go status is configured as required.
- Changes to Tauri capabilities must remain limited to the main window and the
  minimum HTTP, save-dialog, file-write, and external-URL operations required by
  the desktop client.
- Release candidates require a successful signed platform build and a manual
  launch smoke check on each distributed operating system.
