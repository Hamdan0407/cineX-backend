# CineX Render Redis Configuration

## Purpose

CineX seat holds are distributed Redis locks. Redis is required in production: CineX does not fall back to local memory when Redis is unavailable.

## Required Render Variables

Set these variables on the **backend Render service** and redeploy it:

| Variable | Required value |
| --- | --- |
| `REDIS_URL` | The complete connection URL supplied by the Redis provider, for example `redis://:password@host:6379/0`. |
| `REDIS_SSL_ENABLED` | `true` when the provider URL uses `rediss://`/TLS; otherwise `false`. |
| `REDIS_REQUIRED` | `true`; prevents a production cache fallback if the configured Redis service is unavailable. |
| `SEAT_LOCK_BACKEND` | `redis` |
| `CLERK_ISSUER` | The exact Clerk instance issuer URL. |
| `CLERK_JWKS_URL` | The Clerk JWKS endpoint for the same instance. |
| `CINEX_FRONTEND_URL` | `https://cinextickets.in` |

`REDIS_URL` is the preferred production configuration. It includes the hostname, port, database, and credentials without exposing them in source control. Do not set secrets in the repository or browser environment.

## Alternative Configuration

If the provider gives individual values instead of a URL, leave `REDIS_URL` empty and set all of these:

| Variable | Required value |
| --- | --- |
| `REDIS_HOST` | Redis hostname reachable from Render. |
| `REDIS_PORT` | Redis port, usually `6379`. |
| `REDIS_PASSWORD` | Redis password, if required. |
| `REDIS_SSL_ENABLED` | `true` for TLS-enabled Redis; otherwise `false`. |

## Verification

1. Redeploy the backend after updating the variables.
2. Open Render logs and confirm startup does not report a Redis connection failure.
3. Check the backend health endpoint: `/actuator/health` must report Redis as `UP`.
4. Sign in through Clerk, select one available seat, and confirm the lock request returns `200` with `true`.
5. In a second signed-in session, select the same seat and confirm it returns `409`.
6. Close or cancel the first selection, then confirm the second session can acquire the seat.

If the health check reports Redis as down, do not disable seat locking or set `SEAT_LOCK_BACKEND=memory` in production. Verify the Redis provider allows inbound connections from Render and that the TLS setting matches the provider URL.
