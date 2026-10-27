# Variation Income End-To-End Regression

## Automated Coverage

`VariationIncomeLifecycleIntegrationTest` runs against an empty Testcontainers
PostgreSQL database through the real HTTP API and uses a temporary local file
store. It verifies the following operational flow:

1. Create a project and project section with a contract amount.
2. Create a draft variation order and upload a real text evidence file through
   the multipart file API.
3. Bind the uploaded file to the variation, then submit it and verify that its
   estimated amount appears as pending variation income on the project dashboard.
4. Confirm the variation with a different commercial amount and verify the
   attachment, confirmation history, confirmed variation income, total revenue,
   and contract-derived receivable amount.

Run the focused regression from the repository root:

```bash
JAVA_HOME=/Users/h/code/env/jdk-21/Contents/Home \
  /Users/h/code/env/maven-3.9/bin/mvn -B -ntp -f server/pom.xml \
  -Dtest=VariationIncomeLifecycleIntegrationTest test
```

Run the full backend suite before merging a change that affects project
variation, attachments, confirmation, or dashboard revenue:

```bash
JAVA_HOME=/Users/h/code/env/jdk-21/Contents/Home \
  /Users/h/code/env/maven-3.9/bin/mvn -B -ntp -f server/pom.xml test
```

## Web And Miniapp Smoke Record

Use local or test fixtures only. Record the result in the pull request using
[`REGRESSION_EVIDENCE_RECORD_TEMPLATE.md`](REGRESSION_EVIDENCE_RECORD_TEMPLATE.md).

| Step | Role | Route or action | Expected result | Result / evidence |
| --- | --- | --- | --- | --- |
| Create a variation draft | Project member | Miniapp variation form or Web Variations | Project, section, date, type, reason, and estimated amount are retained in a draft | |
| Upload and bind evidence | Project member | Miniapp variation form or Web Variations | A permitted file is attached as `photo` or `evidence`, and appears in variation evidence | |
| Submit variation | Project member | Miniapp variation form or Web Variations | Status changes to `submitted`; project dashboard shows the estimated amount as pending variation income | |
| Confirm commercial amount | ADMIN or MANAGER | Web Variations or approval todo | Confirmed amount and remark are recorded; confirmation history is visible | |
| Check project dashboard | Project finance role | Project dashboard | Pending variation becomes zero; confirmed variation, revenue breakdown, and receivable amount reflect the confirmed amount | |

Run these local quality checks before recording a passing smoke result:

```bash
cd clients/desktop
pnpm run build:renderer
cd ../miniapp
pnpm run check
```

Do not attach production project data, location details, phone numbers, or
unredacted contract evidence. The governing evidence and exception rules are in
[`REGRESSION_EVIDENCE_POLICY.md`](REGRESSION_EVIDENCE_POLICY.md).
