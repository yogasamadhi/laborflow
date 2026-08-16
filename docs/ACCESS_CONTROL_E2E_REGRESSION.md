# Access Control End-To-End Regression

## Automated Coverage

`AccessControlIntegrationTest` runs against an empty Testcontainers PostgreSQL
database through the real HTTP API. It verifies the following authorization and
sensitive-data boundaries:

1. Anonymous access to system-user management returns `401`.
2. An employee can read only their own salary detail, while another employee's
   detail is rejected; system-user access and monthly-salary export return `403`.
3. An active project member can create a variation for the assigned project,
   while a non-member is rejected for the same project.
4. FINANCE cannot approve a monthly salary, MANAGER cannot list monthly salary,
   and the ADMIN user list contains neither a password field nor last-login IP.

Run the focused regression from the repository root:

```bash
JAVA_HOME=/Users/h/code/env/jdk-21/Contents/Home \
  /Users/h/code/env/maven-3.9/bin/mvn -B -ntp -f server/pom.xml \
  -Dtest=AccessControlIntegrationTest test
```

Run the full backend suite before merging a change that affects authentication,
role expressions, project scope, salary access, user serialization, or exports:

```bash
JAVA_HOME=/Users/h/code/env/jdk-21/Contents/Home \
  /Users/h/code/env/maven-3.9/bin/mvn -B -ntp -f server/pom.xml test
```

## Web Smoke Record

Use local or test accounts only. Record the result with
[`REGRESSION_EVIDENCE_RECORD_TEMPLATE.md`](REGRESSION_EVIDENCE_RECORD_TEMPLATE.md).

| Step | Role | Route or action | Expected result | Result / evidence |
| --- | --- | --- | --- | --- |
| Open a hidden route | EMPLOYEE | Directly open `/system/users` | A readable no-access state is shown and the return action opens `/dashboard` | |
| Expire a token | Any role | Reload any protected route with an expired token | Local session is cleared and login states that the session expired | |
| Trigger a forbidden operation | FINANCE | Attempt monthly-salary approval | The page remains usable and shows the server's no-permission message | |
| Check allowed route | FINANCE | Open `/projects/team-settlements` | The route and its permitted API data remain accessible | |

Run the Web checks before recording a passing smoke result:

```bash
cd clients/desktop
bun run typecheck
bun run test
bun run build:renderer
```

Do not attach production user names, employee information, salary details, IP
addresses, tokens, or browser storage. The governing evidence and exception
rules are in [`REGRESSION_EVIDENCE_POLICY.md`](REGRESSION_EVIDENCE_POLICY.md).
