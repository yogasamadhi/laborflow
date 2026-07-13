# Web CI Gate

## Scope

`.workflow/PRWebPipeline.yml` defines the Gitee Go quality job for pull
requests targeting `master`. The job uses Node.js 20.18.0 and the pnpm version
pinned in `web/package.json`, then runs:

```bash
pnpm --dir web install --frozen-lockfile
pnpm --dir web typecheck
pnpm --dir web test
pnpm --dir web build
```

`typecheck` performs strict TypeScript validation. `test` runs the checked-in
Vitest suite, beginning with pure UI utility behavior. `build` validates the
production Vite bundle.

## Enablement

After importing the pipeline in Gitee Go, the repository administrator must
run it once against a pull request to `master`, then make `PR Web Quality` a
required successful status in the protected-branch settings. Dependency
installation must use `--frozen-lockfile`; a lockfile mismatch is a failed
quality gate, not something the CI job should repair.

## Contract

- Pull requests into `master` run type checking, tests, and production build.
- Any failure blocks merge after the status is configured as required in Gitee.
- New Web behavior should add focused Vitest coverage beside the tested module
  or in the established test directory before the related pull request merges.
