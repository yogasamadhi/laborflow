# Docs And Diff CI Gate

## Scope

`.workflow/PRDocsPipeline.yml` defines the Gitee Go pull-request gate for
`master`. It fetches the target branch and runs:

```bash
git diff --check origin/master...HEAD
node scripts/ci/check-docs.mjs
```

The Node checker validates local Markdown links across repository-maintained
Markdown files while skipping dependencies and build output, then enforces
Flyway migration filenames in
`server/src/main/resources/db/migration` as `V<number>__lowercase_name.sql`
with no duplicate version number. External URLs and Markdown anchors are not
treated as local files.

## Enablement

Import the workflow in Gitee Go, run it once for a pull request targeting
`master`, then require the successful `PR Docs And Diff Check` status in the
protected-branch settings. Whitespace errors, broken local documentation links,
invalid migration names, and duplicate migration versions must block merge.

## Local Verification

```bash
git diff --check origin/master...HEAD
node scripts/ci/check-docs.mjs
```

For a new branch without a local remote comparison point, run `git diff --check`
and the Node checker before commit, then use the range command after fetching
`origin/master`.
