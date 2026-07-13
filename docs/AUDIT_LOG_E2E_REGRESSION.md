# Audit Log End-To-End Regression

## Automated Coverage

`AuditLogLifecycleIntegrationTest` runs against an empty Testcontainers
PostgreSQL database through the real HTTP API. It performs the following actions
as the bootstrap ADMIN user, then queries `/api/system/logs` and verifies the
module, action, business ID, operator, and non-empty summary of every record:

1. Create and update a system user.
2. Create, update, generate, submit, reject, approve, pay, and export a monthly
   salary.
3. Submit and reject one variation order, then submit and commercially confirm a
   second variation order.
4. Create and update a compensation deduction, then confirm a performance score.
5. Bind and unbind a fixture WeChat session through the administrator API.

Run the focused regression from the repository root:

```bash
JAVA_HOME=/Users/h/code/env/jdk-21/Contents/Home \
  /Users/h/code/env/maven-3.9/bin/mvn -B -ntp -f server/pom.xml \
  -Dtest=AuditLogLifecycleIntegrationTest test
```

Run the full backend suite before merging a change that affects operation logs,
salary actions or exports, project approvals, compensation confirmation, system
users, or WeChat bindings:

```bash
JAVA_HOME=/Users/h/code/env/jdk-21/Contents/Home \
  /Users/h/code/env/maven-3.9/bin/mvn -B -ntp -f server/pom.xml test
```

## Administrator Smoke Record

Use local or test data only. Record the result with
[`REGRESSION_EVIDENCE_RECORD_TEMPLATE.md`](REGRESSION_EVIDENCE_RECORD_TEMPLATE.md).

| Step | Role | Route or action | Expected result | Result / evidence |
| --- | --- | --- | --- | --- |
| Query system audit log | ADMIN | Web System / operation log or `GET /api/system/logs` | User, salary, project, compensation, and WeChat entries show action, operator, business ID, summary, and time | |
| Check salary evidence | ADMIN | Monthly salary detail / audit trail | Create, submit, approve or reject, payment, and export actions remain traceable | |
| Check project evidence | ADMIN | Variation or daily-report approval history plus operation log | Submit, confirmation, and rejection record the correct business item and operator | |
| Check WeChat evidence | ADMIN | System / WeChat bindings | Binding and unbinding appear in the operation log after the state change | |

Do not attach production user names, salary amounts, WeChat openids, tokens, or
unredacted operation-log exports. The governing evidence and exception rules are
in [`REGRESSION_EVIDENCE_POLICY.md`](REGRESSION_EVIDENCE_POLICY.md).
