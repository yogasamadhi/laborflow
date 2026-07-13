# File Access And Attachment Boundary Regression

## Automated Coverage

`FileAccessBoundaryIntegrationTest` runs against an empty Testcontainers
PostgreSQL database through the real HTTP API and uses a temporary local private
file store. It verifies the following boundaries:

1. A project member uploads a real text file and binds it to a daily report.
2. The uploader, another active member of that project, and the administrator
   can read file metadata and download the stored content. A member assigned
   only to a different project is rejected for both metadata and download.
3. An administrator cannot reuse the daily-report evidence as a variation-order
   attachment, including for another project.
4. A private uploaded file can be used as a salary payment proof, but cannot
   then be archived as a project document.

The binding guard permits reuse inside one business domain when the workflow
requires it, such as one batch salary proof covering multiple salary details.
It rejects reuse across daily reports, variation orders, project payment
vouchers, document archives, and salary payment proofs. Attachment writes take
a transaction-scoped lock on the file row before checking existing domains.

Run the focused regression from the repository root:

```bash
JAVA_HOME=/Users/h/code/env/jdk-21/Contents/Home \
  /Users/h/code/env/maven-3.9/bin/mvn -B -ntp -f server/pom.xml \
  -Dtest=FileAccessBoundaryIntegrationTest test
```

Run the full backend suite before merging a change that affects file storage,
file authorization, project attachments, payment proofs, or project archives:

```bash
JAVA_HOME=/Users/h/code/env/jdk-21/Contents/Home \
  /Users/h/code/env/maven-3.9/bin/mvn -B -ntp -f server/pom.xml test
```

## Manual File Smoke Record

Use local or test accounts and files only. Record the result with
[`REGRESSION_EVIDENCE_RECORD_TEMPLATE.md`](REGRESSION_EVIDENCE_RECORD_TEMPLATE.md).

| Step | Role | Route or action | Expected result | Result / evidence |
| --- | --- | --- | --- | --- |
| Upload private evidence | Project member | `POST /api/files/upload` then a daily-report attachment action | The uploaded file returns an `/api/files/{id}/download` path and appears only on the selected daily report | |
| Download through project access | Different active member of the same project | `GET /api/files/{id}` and `/api/files/{id}/download` | Metadata and original content are returned | |
| Reject unrelated access | Member assigned only to another project | `GET /api/files/{id}` and `/api/files/{id}/download` | Both requests are rejected with the no-download message | |
| Reject cross-domain reuse | ADMIN or permitted project role | Attach a bound daily-report file to a variation, payment voucher, document archive, or salary proof | The write is rejected with `附件已被其他业务使用` and creates no second attachment | |
| Keep valid batch proof behavior | Salary manager | Batch-mark two salary details paid with one fresh uploaded proof | Both details retain the same private proof URL and the batch succeeds | |

Do not attach production files, employee names, project details, salary values,
private download URLs, tokens, or unredacted binary evidence. The governing
evidence and exception rules are in
[`REGRESSION_EVIDENCE_POLICY.md`](REGRESSION_EVIDENCE_POLICY.md).
