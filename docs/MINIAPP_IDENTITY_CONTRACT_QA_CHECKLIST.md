# Miniapp Identity And Contract QA Checklist

## Scope

This checklist closes the miniapp identity, session, and shared service
contract baseline. It does not change business state machines, salary rules,
project pricing, or the backend API surface.

Run the automated checks before DevTools verification:

```bash
npm --prefix legacy/miniapp run check
JAVA_HOME=/Users/h/code/env/jdk-21/Contents/Home /Users/h/code/env/maven-3.9/bin/mvn -B -ntp -f server/pom.xml test
```

The miniapp test runs with mocked `wx` APIs and verifies cache normalization,
401/403 behavior, authentication service paths and payloads, and daily-report
and variation attachment types. The backend suite verifies the checked-in core
API contract against an isolated PostgreSQL container.

## Contract Surface

| API | Request contract | Response data used by miniapp | Client behavior |
| --- | --- | --- | --- |
| `POST /api/auth/wechat/session` | `code` is required | `openid`, `bound`, optional binding identity | Obtain a current WeChat identity before account login; show whether it is already bound. |
| `POST /api/auth/login` | `username`, `password`, optional `wechatOpenid`, `wechatNickname` | `token`, `expiresAt`, `userId`, `employeeId`, `username`, `nickname`, `roles` | Persist a session only when token, user ID, and username are complete. Do not send the removed `wechatCode` field. |
| `GET /api/auth/me` | Bearer token | `userId`, `employeeId`, `username`, `nickname`, `roles` | Refresh cached profile while retaining the existing token. |
| `POST /api/auth/logout` | Bearer token when present | Result envelope acknowledgement | Always clear local token and user cache, including when the request is unavailable. |
| `POST /api/daily-work-reports/{id}/files` | `fileId`, `fileType: photo` | Result envelope | Use a file type permitted by `FileAttachDTO`; `site_photo` is invalid. |
| `POST /api/variation-orders/{id}/files` | `fileId`, `fileType: photo` | Result envelope | Use the same permitted photo type. |

## HTTP And Cache Expectations

| Situation | Expected client result | Cache result |
| --- | --- | --- |
| `2xx` with `code: 0` | Resolve `data` | Keep the current valid session. |
| Authenticated request returns `401` | Show login-expired feedback and relaunch login | Clear both `laborflow_token` and `laborflow_user`. |
| Request returns `403` | Show the server denial message or a no-permission message | Keep the current session; do not relaunch login. |
| Login request returns `401` | Show the credential error on the login page | Do not relaunch or wipe an existing cache as a side effect. |
| Network failure or unreadable upload result | Show a retry-oriented error | Keep the session unless the server explicitly returned authenticated `401`. |
| Cached token/user is incomplete or malformed | Treat the user as logged out | Remove both cache entries before a protected page renders. |

## DevTools Smoke Record

Use only a local or test backend and non-production identities. Record the
metadata and evidence reference in
[`MINIAPP_SMOKE_EVIDENCE_TEMPLATE.md`](MINIAPP_SMOKE_EVIDENCE_TEMPLATE.md).

| Case | Setup and action | Expected evidence |
| --- | --- | --- |
| Account login | Obtain a WeChat session, log in with a valid account, then reopen the miniapp. | Home page renders with the correct name/role; token and user cache survive a normal reopen. |
| Existing binding | Use an account already bound to the current WeChat identity. | Login page shows the bound state without trying to overwrite the account relationship. |
| Login denial | Submit incorrect credentials. | Error remains on login page; no looped relaunch and no unrelated session deletion. |
| Expired token | Use a test token rejected with `401`, then enter a protected page or upload a file. | Both cache keys are removed and the app returns to login once. |
| No permission | Use a valid account that receives `403` for a protected action or upload. | Denial message is visible; the current session remains usable on allowed pages. |
| Profile refresh and logout | Refresh `/auth/me`, then log out while online and under weak-network simulation. | Refresh retains the token; logout clears cache and returns to login even if the remote acknowledgement cannot complete. |
| Attachment contract | Upload a daily-report photo and a variation photo to test fixtures. | Both attachment requests use `fileType: photo`; server accepts the payload. |
| Weak network | Enable weak-network simulation for login, profile refresh, and file upload. | Retry-oriented error text appears; no malformed cache is left behind. |

This document defines verification steps only. It does not claim that a live
DevTools, device, binding, upload, or weak-network run has already occurred.
