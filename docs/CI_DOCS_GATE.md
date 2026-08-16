# Docs And Diff Local Quality Gate

## Scope

The documentation gate uses Bun and Git directly; no hosted workflow is
required. Run from the repository root:

```bash
git diff --check
bun scripts/ci/check-docs.mjs
```

For a branch-range check after fetching the integration branch:

```bash
git diff --check origin/main...HEAD
bun scripts/ci/check-docs.mjs
```

The Bun checker validates local Markdown links while skipping dependencies and
build output. It also enforces Flyway filenames as
`V<number>__lowercase_name.sql`, rejects duplicate migration versions, and
checks that environment variables referenced by `application.yml` are present
in `.env.example`.

## Contract

- Whitespace errors and broken local documentation links fail the gate.
- Invalid or duplicate Flyway migration versions fail the gate.
- An `application.yml` environment variable missing from `.env.example` fails the gate.
- External URLs and Markdown anchors are not treated as local files.
