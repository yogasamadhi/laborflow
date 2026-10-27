# Regression Evidence Policy

## Purpose

This policy makes regression evidence part of pull-request review for changes
that can affect payroll, project settlement, authorization, or data migration.
The evidence is recorded in the pull request so it remains discoverable with
the reviewed change and its merge history.

## Required Pull-Request Content

Every pull request targeting `main` uses
[`REGRESSION_EVIDENCE_RECORD_TEMPLATE.md`](REGRESSION_EVIDENCE_RECORD_TEMPLATE.md).
The author selects all affected risk categories and records the executed checks,
result, and an evidence reference in the pull-request description.

The author must attach a completed record based on
[`REGRESSION_EVIDENCE_RECORD_TEMPLATE.md`](REGRESSION_EVIDENCE_RECORD_TEMPLATE.md)
when the change affects any of the following:

- Salary calculation, payslips, payment, deductions, compensation sources, or
  payroll export.
- Projects, daily reports, variations, quantities, team settlement, or project
  payments.
- Roles, permissions, data scope, sensitive data, private attachments, or audit
  access.
- Flyway migrations, data upgrades, schema compatibility, or historical data.

For a small change, the completed record may be included directly in the
pull-request description. For a longer manual run, attach it as a pull-request
comment or approved QA artifact and link it from the description. Keep the
evidence with the pull request rather than committing test-account details or
screenshots to the source tree.

## Evidence Quality

Each required record identifies the pull request or commit, test environment,
fixture label, test role, scenario, expected result, actual result, and an
evidence reference. It must show the checks that are meaningful for the
selected risk:

- Salary changes cover the affected payroll state transition and amount or
  export outcome using non-production test data.
- Project changes cover the affected flow from entry through confirmation,
  settlement, approval, or payment as applicable.
- Permission changes cover an allowed role and a denied or out-of-scope role.
- Migration changes cover the migration command or integration test and the
  important legacy-data invariants.

Automated command output can be referenced by CI run, local log excerpt, or
test artifact. Manual evidence can be a redacted screenshot, screen recording,
or reproducible step record. A link alone is not enough when it does not state
which scenario passed or failed.

## Not-Applicable Exceptions

A selected required category may be marked not applicable only when the author
states why the change cannot affect its behavior, data, authorization, or
compatibility boundary. The reviewer records acceptance in the pull request.
"Not tested", "small change", or "no time" is not an acceptable exception.

## Data Handling

Evidence must use local or test data. Never include passwords, tokens, cookies,
phone numbers, openid values, ID numbers, bank-account data, unredacted salary
amounts, production exports, or production screenshots. Redact any incidental
personal or financial data before attaching evidence.

## Review Gate

Before approval, the reviewer confirms:

1. The selected risk categories match the diff.
2. Required evidence is present, readable, and covers the affected flow.
3. Any exception gives a concrete non-impact explanation and is explicitly
   accepted.
4. The evidence contains no secret, personal, or production data.

Missing or insufficient evidence blocks merge until it is completed or an
approved exception is recorded. This manual review gate complements the local
automated checks documented in the quality gate documents.
