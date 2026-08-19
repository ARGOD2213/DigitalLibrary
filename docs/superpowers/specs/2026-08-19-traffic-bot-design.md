# Local Traffic Bot — Design

**Status:** Approved for planning
**Date:** 2026-08-19
**Scope:** A standalone dev tool for local use only. Not part of the deployed app, not a RAG sub-project — but it's the thing that will generate the realistic traffic and logs the [Log Assistant RAG](2026-08-19-log-assistant-rag-design.md) is designed to analyze.

## Problem

The project is moving from one machine (where it was originally built) to another next week, and needs to be in a solid, working local-dev state with room left for incremental improvement — not a finished product. Beyond that, the local database currently only has the handful of records `DataInitializer` seeds on startup (3 users, 4 books). There's no way to experience how the app behaves with realistic data volume, concurrent users, or under sustained traffic — which matters because the next planned step (the log-assistant RAG, and eventual payments/S3 RAGs) needs real logs and real latency patterns to be useful, not a quiet idle server.

This spec covers a bot that runs against the local app and continuously drives it through realistic user journeys — registering, browsing, subscribing, paying, reviewing, publishing as a vendor, approving as an admin — until stopped, so the developer can watch the system behave like a live multi-tenant app: data accumulating, requests flowing, latency and errors visible in real time.

## Non-goals

- Not a correctness test suite. It doesn't assert expected outcomes — it's a traffic generator, not a test runner. (Existing backend tests, if any, are a separate concern.)
- Not a production/staging load-testing tool. It refuses to run against anything but `localhost`/`127.0.0.1` (see Safety, below) — this is explicitly the opposite of a tool meant to point at a real deployment.
- Not a raw throughput/stress benchmark. Pacing is deliberately realistic (small random delays between a virtual user's actions), not "as fast as possible" — see the approved design decision on load intensity.
- Doesn't touch the Spring Boot backend or React frontend code at all. It's an external HTTP client, same as a browser or curl.

## Architecture

```
tools/traffic-bot/
  index.js            entry point: parse config, start workers, handle SIGINT
  config.js            env-driven config (target URL, concurrency, pool size, pacing)
  safety.js             refuses to start unless target URL is localhost/127.0.0.1
  userPool.js            manages the growing pool of virtual users (register/login)
  journeys/
    reader.js             browse -> favorite -> subscribe -> checkout -> review
    vendor.js              apply -> (post-approval) add book -> check dashboard
    admin.js                approve pending vendors -> check audit logs
  apiClient.js           thin axios wrapper: records latency/status per call
  stats.js                 aggregates latency percentiles + error rate, prints every 10s
  bootstrap.js            ensures at least one subscription plan exists (admin-seeded)
```

Each of the ~8 (configurable) concurrent workers loops indefinitely: pick or create a virtual user from the pool (weighted toward reusing existing users over creating new ones once the pool is warm), pick a journey (weighted: mostly reader, occasional vendor, rare admin), run it to completion with small randomized pauses between steps, record results, repeat. The user pool is capped (default 200) so growth is bounded even across a very long-running session — the *volume* of data instead comes from books, orders, payments, reviews, and subscriptions accumulating from repeated actions by the same pool, which is also more realistic than an ever-growing user count.

## Components

### User pool (`userPool.js`)

- Maintains an in-memory list of `{ email, password, role }` for virtual users created so far.
- A worker either reuses an existing pooled user (logs in) or, while under the pool cap, registers a new one (via `@faker-js/faker` for name/email) and adds it to the pool. The reuse-vs-create ratio shifts toward reuse as the pool fills, so early runs grow the pool quickly and later runs are dominated by realistic returning-user logins.
- Vendor-role users are tracked separately (a sub-pool) since they follow a different journey and need admin approval before their vendor actions unlock.

### Bootstrap (`bootstrap.js`)

`DataInitializer` on the backend seeds users and books, but **no subscription plan** — `GET /api/subscriptions/plans` returns empty on a fresh database, which would strand the reader journey's subscribe step with nothing to subscribe to. On startup, the bot logs in as the seeded admin (`admin@library.com`) and creates one or two subscription plans via `POST /api/subscriptions/plans` if none exist yet (idempotent, same "create if missing" idiom as `DataInitializer` itself).

### Journeys

- **Reader** (weighted most common): login-or-register → `GET /api/books` (browse/search) → `POST /api/books/{id}/favorite` → `GET /api/subscriptions/plans` → `POST /api/subscriptions/subscribe/{planId}` → `POST /api/payments/checkout` with `paymentGateway: "MOCK"` (succeeds immediately — no webhook signing needed, confirmed against `PaymentServiceImpl`) → `POST /api/books/{id}/reviews`.
- **Vendor** (occasional): login-or-register → `POST /api/vendors/apply` → (on a later loop, once approved) `POST /api/books` with faker-generated title/author/price → `GET /api/vendors/me/commissions`.
- **Admin** (rare, always the seeded `admin@library.com`): `GET /api/vendors/pending` → `PUT /api/vendors/{id}/approve` for each pending application → `GET /api/admin/audit-logs`. This is also what unblocks the vendor journey's second half.

Each journey step is independent and skippable on failure (e.g. a subscribe call failing because the user already has a subscription doesn't abort the whole journey) — a worker logs the failure to its stats and moves to the next step or the next loop iteration, since the goal is sustained traffic, not an all-or-nothing transaction.

### Safety guardrail (`safety.js`)

Reads the target base URL from config (default `http://localhost:8000`). If the resolved host is not `localhost` or `127.0.0.1`, the bot refuses to start and exits with an error — no flag silently overrides this by default. An explicit `--force` CLI flag is required to bypass it, and using it prints a loud warning. This exists specifically because this project's `.env` was found pointing at production credentials earlier during local setup — the bot must never be the thing that accidentally generates fake traffic against a real deployment.

### Observability (`stats.js`)

Every 10 seconds, prints a snapshot to the console: total requests, error rate, and p50/p95/p99 latency, broken down by endpoint. On `SIGINT` (Ctrl+C), the bot stops starting new actions, lets in-flight requests finish, prints a final summary (same shape, plus total entities created: users, books, orders, payments, reviews), and exits cleanly. This is the primary way the developer experiences "how everything works" — latency, error rate, and throughput are visible live, not something to dig for in logs afterward. The backend's own logs accumulate naturally alongside this and are what the log-assistant RAG will eventually read.

## Error handling

- **A journey step fails** (validation error, 401, 409, etc.): recorded in stats as an error for that endpoint; the worker moves on rather than crashing. A single bad response should never take down a worker.
- **The backend is unreachable at startup**: the bot retries a few times with backoff, then exits with a clear "is the backend running?" message rather than spinning forever against a dead target.
- **The backend goes down mid-run**: workers' requests start failing and erroring into stats (visible immediately in the next 10s snapshot); the bot keeps running and retries on its normal loop cadence rather than treating a transient outage as fatal — it should recover automatically if the backend comes back.
- **Target URL safety check fails**: fatal, exits immediately before any traffic is sent (see Safety, above).

## Testing

- Manual verification: run the bot against the local stack for a few minutes, confirm the console stats show non-zero traffic across all three journey types, confirm new books/orders/payments/reviews actually appear via the existing API (e.g. `GET /api/books` count growing), confirm Ctrl+C stops cleanly with a final report.
- Verify the safety guard: attempt to run with a non-localhost target URL (without `--force`) and confirm it refuses to start.
- No automated test suite — this is a dev tool, not shipped code; its own correctness is verified by using it, not by a test harness testing a test harness.

## Future work (explicitly out of scope here)

- Feeding the bot's own summary reports (or the backend logs it generates) into the log-assistant RAG once that's built.
- Configurable journey weighting / scenario presets (e.g. a "payment failure storm" mode) if plain realistic traffic turns out to be insufficient for a specific investigation later.
- Any of the improvements the user plans to build incrementally on their own machine going forward — this spec deliberately stops at "a working, realistic local environment to build on," not a finished feature set.
