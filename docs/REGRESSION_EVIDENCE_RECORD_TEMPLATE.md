# Regression Evidence Record Template

Use this record for a pull request that changes payroll, project flows,
authorization, or migrations. Complete it in the pull request, a linked QA
artifact, or a pull-request comment. Do not commit completed records containing
test-account details or screenshots to the repository.

## Run Metadata

| Field | Record |
| --- | --- |
| Pull request / commit | |
| Tester | |
| Date and time | |
| Environment | Local / test only |
| Build or version | |
| Fixture label | |
| Test role and account label | |
| Related issue or UAT item | |

## Changed Boundary

| Category | Affected behavior or data | Selected / not applicable | Reason when not applicable |
| --- | --- | --- | --- |
| Salary or payroll | | | |
| Project or settlement | | | |
| Permission or sensitive data | | | |
| Migration or historical data | | | |

## Verification Results

| Scenario | Preconditions and steps | Expected result | Actual result | Pass / fail | Evidence reference |
| --- | --- | --- | --- | --- | --- |
| Automated check | | | | | |
| Primary affected flow | | | | | |
| Allowed role or data scope | | | | | |
| Denied role or out-of-scope access | | | | | |
| Error, retry, or boundary behavior | | | | | |

## Migration Details

Complete this section when a migration, data upgrade, schema change, or legacy
compatibility boundary is selected.

| Check | Record |
| --- | --- |
| Source database or fixture version | |
| Migration command or integration test | |
| Migration result | Pass / fail |
| Core data invariants checked | |
| Rollback or recovery consideration | |

## Result And Review

| Field | Record |
| --- | --- |
| Overall result | Pass / fail / blocked |
| Defects, deviations, or follow-up | |
| Retest commit and result | |
| Reviewer | |

Use non-production fixtures and redacted evidence only. Do not record a
password, token, cookie, phone number, openid value, ID number, bank-account
data, unredacted salary value, production record, or production screenshot.
