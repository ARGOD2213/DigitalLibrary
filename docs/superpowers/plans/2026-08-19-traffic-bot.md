# Local Traffic Bot Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a standalone Node.js dev tool (`tools/traffic-bot/`) that drives the locally-running Digital Library backend through realistic reader/vendor/admin journeys until stopped, so the developer can see the app behave under real concurrent traffic — data accumulating, requests flowing, latency and errors visible live.

**Architecture:** A CLI entry point starts N concurrent async "workers." One worker (if concurrency ≥ 2) runs an admin sweep loop (approve pending vendors); the rest each loop indefinitely picking a virtual user from a capped, growing pool and running a weighted-random reader or vendor journey against the backend's real REST API, with small randomized delays between steps. Every HTTP call goes through a thin axios wrapper that records latency/status into an in-memory stats aggregator, which prints a snapshot every 10s and a final report on Ctrl+C.

**Tech Stack:** Node.js (v22, already required for the frontend), axios, @faker-js/faker (MIT). Node's built-in `node:test` + `node:assert` for the handful of pure-logic unit tests — no new test-framework dependency.

**Spec:** `docs/superpowers/specs/2026-08-19-traffic-bot-design.md`

---

## Chunk 1: Scaffolding and plumbing

Tasks 1–6: package setup plus the pure, independently-testable building blocks (config, safety guard, stats, API client, user pool) with no journey logic yet.

### File Structure

This is the final tree the whole plan builds toward. Chunk 1 (Tasks 1–6 below) creates only `package.json`, `.gitignore`, `config.js`, `safety.js` + test, `stats.js` + test, `apiClient.js`, and `userPool.js` + test; the rest (`bootstrap.js`, `journeys/`, `index.js`, `README.md`) are Chunk 2.

```
tools/traffic-bot/
  package.json          deps: axios, @faker-js/faker; "start" script
  .gitignore             node_modules
  config.js               env-driven config, exported as a plain object
  safety.js                isLocalTarget(url) pure check + assertSafeTarget(config)
  safety.test.js
  stats.js                  Stats class: record(), snapshot(), percentile()
  stats.test.js
  apiClient.js             axios wrapper: timed request(), login(), register()
  userPool.js               UserPool class: getOrCreateUser(), weighting logic
  userPool.test.js          (pure weighting-decision test, HTTP calls excluded)
  bootstrap.js             ensureSubscriptionPlan(adminClient)
  journeys/
    reader.js                runReaderJourney(client, faker)
    vendor.js                 runVendorJourney(client, faker)
    admin.js                   runAdminSweep(adminClient)
  index.js                 entry point: parse argv/env, wire everything, workers, SIGINT
  README.md                how to run it, what it does, config table
```

Each file has one job: `config.js` never touches the network, `safety.js` never touches config internals beyond reading a URL string, `stats.js` knows nothing about HTTP, `apiClient.js` knows nothing about journeys, `journeys/*.js` know nothing about worker scheduling, `index.js` is the only file that wires the pieces together.

### Verified backend contract (from the approved spec)

- `POST /api/auth/register` — `{ username, fullName, email, password }` → creates `ROLE_USER`.
- `POST /api/auth/login` — `{ email, password }` → `{ data: { accessToken, ... } }`.
- `GET /api/books?page=&size=` — public.
- `POST /api/books/{id}/favorite` — auth required.
- `GET /api/subscriptions/plans` — public, **empty on a fresh DB** (`DataInitializer` doesn't seed one).
- `POST /api/subscriptions/plans` — `hasRole('ADMIN')`, body per `CreateSubscriptionPlanRequest`.
- `POST /api/subscriptions/subscribe/{planId}` — auth required, always succeeds (cancels+recreates).
- `POST /api/payments/checkout` — auth required, body `{ bookIds: [id,...], paymentGateway: "MOCK" }`, succeeds synchronously.
- `POST /api/books/{id}/reviews` — auth required.
- `POST /api/vendors/apply` — auth required, body per `VendorApplicationRequest`.
- `GET /api/vendors/pending` — `hasRole('ADMIN')`.
- `PUT /api/vendors/{id}/approve` — `hasRole('ADMIN')`, promotes the applicant to `ROLE_VENDOR`.
- `POST /api/books` — `hasAnyRole('ADMIN','VENDOR')`.
- `GET /api/vendors/me/commissions` — `hasRole('VENDOR')`.
- Seeded admin: `admin@library.com` / `admin123`.

---

### Task 1: Scaffold the tool and its dependencies

**Files:**
- Create: `tools/traffic-bot/package.json`
- Create: `tools/traffic-bot/.gitignore`

- [ ] **Step 1: Create the directory and initialize the package**

Run:
```powershell
cd "tools"
mkdir traffic-bot
cd traffic-bot
npm init -y
```

- [ ] **Step 2: Install dependencies**

Run: `npm install axios@^1.19.0 @faker-js/faker@^9`

Expected: `package.json` gains a `dependencies` block with `axios` and `@faker-js/faker`; `node_modules/` and `package-lock.json` are created.

- [ ] **Step 3: Edit `package.json` to add a start script and mark it a module**

```json
{
  "name": "digital-library-traffic-bot",
  "version": "1.0.0",
  "private": true,
  "type": "module",
  "description": "Local-only dev tool that drives the running Digital Library app through realistic traffic. Refuses to run against anything but localhost.",
  "scripts": {
    "start": "node index.js",
    "test": "node --test"
  },
  "dependencies": {
    "@faker-js/faker": "^9.0.0",
    "axios": "^1.19.0"
  }
}
```

- [ ] **Step 4: Add `.gitignore`**

```
node_modules/
```

- [ ] **Step 5: Commit**

```bash
git add tools/traffic-bot/package.json tools/traffic-bot/.gitignore tools/traffic-bot/package-lock.json
git commit -m "traffic-bot: scaffold Node package with axios and faker"
```

---

### Task 2: `config.js` — env-driven configuration

**Files:**
- Create: `tools/traffic-bot/config.js`

- [ ] **Step 1: Write the file**

```js
// config.js — all tunables in one place, read once at startup.

function intFromEnv(name, fallback) {
  const raw = process.env[name];
  if (raw === undefined || raw === '') return fallback;
  const parsed = Number.parseInt(raw, 10);
  return Number.isNaN(parsed) ? fallback : parsed;
}

export function loadConfig(argv = process.argv.slice(2)) {
  return {
    targetUrl: process.env.TRAFFIC_BOT_TARGET_URL || 'http://localhost:8000',
    concurrency: intFromEnv('TRAFFIC_BOT_CONCURRENCY', 8),
    userPoolCap: intFromEnv('TRAFFIC_BOT_POOL_CAP', 200),
    minDelayMs: intFromEnv('TRAFFIC_BOT_MIN_DELAY_MS', 300),
    maxDelayMs: intFromEnv('TRAFFIC_BOT_MAX_DELAY_MS', 1500),
    adminSweepIntervalMs: intFromEnv('TRAFFIC_BOT_ADMIN_SWEEP_MS', 20000),
    statsIntervalMs: intFromEnv('TRAFFIC_BOT_STATS_INTERVAL_MS', 10000),
    adminEmail: process.env.TRAFFIC_BOT_ADMIN_EMAIL || 'admin@library.com',
    adminPassword: process.env.TRAFFIC_BOT_ADMIN_PASSWORD || 'admin123',
    forceUnsafeTarget: argv.includes('--force'),
  };
}
```

- [ ] **Step 2: Commit**

```bash
git add tools/traffic-bot/config.js
git commit -m "traffic-bot: add env-driven config loader"
```

---

### Task 3: `safety.js` — refuse to run against a non-local target

**Files:**
- Create: `tools/traffic-bot/safety.js`
- Test: `tools/traffic-bot/safety.test.js`

- [ ] **Step 1: Write the failing test**

```js
// safety.test.js
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { isLocalTarget, assertSafeTarget } from './safety.js';

test('isLocalTarget accepts localhost and 127.0.0.1, any port', () => {
  assert.equal(isLocalTarget('http://localhost:8000'), true);
  assert.equal(isLocalTarget('http://127.0.0.1:8000'), true);
  assert.equal(isLocalTarget('https://localhost'), true);
});

test('isLocalTarget rejects everything else', () => {
  assert.equal(isLocalTarget('http://13.233.106.4:8000'), false);
  assert.equal(isLocalTarget('https://digital-library-db.c7aku04iuzxy.ap-south-1.rds.amazonaws.com'), false);
  assert.equal(isLocalTarget('not a url'), false);
});

test('assertSafeTarget throws for a remote URL without force', () => {
  assert.throws(() => assertSafeTarget({ targetUrl: 'http://example.com', forceUnsafeTarget: false }));
});

test('assertSafeTarget does not throw for a remote URL with force', () => {
  assert.doesNotThrow(() => assertSafeTarget({ targetUrl: 'http://example.com', forceUnsafeTarget: true }));
});

test('assertSafeTarget does not throw for localhost', () => {
  assert.doesNotThrow(() => assertSafeTarget({ targetUrl: 'http://localhost:8000', forceUnsafeTarget: false }));
});
```

- [ ] **Step 2: Run the test to verify it fails**

Run (from `tools/traffic-bot/`): `node --test safety.test.js`
Expected: FAIL — `safety.js` does not exist yet (`Cannot find module`).

- [ ] **Step 3: Write the implementation**

```js
// safety.js — this project's local .env was once found pointing at a
// production database with live AWS credentials. This module exists so the
// traffic bot can never be the thing that generates fake load against a
// real deployment: it refuses to start unless the target is localhost.

export function isLocalTarget(targetUrl) {
  let parsed;
  try {
    parsed = new URL(targetUrl);
  } catch {
    return false;
  }
  return parsed.hostname === 'localhost' || parsed.hostname === '127.0.0.1';
}

export function assertSafeTarget(config) {
  if (isLocalTarget(config.targetUrl)) return;
  if (config.forceUnsafeTarget) {
    console.warn(
      `⚠️  --force set: sending traffic to a NON-local target (${config.targetUrl}). ` +
      `This is almost certainly not what you want.`
    );
    return;
  }
  throw new Error(
    `Refusing to run: target "${config.targetUrl}" is not localhost/127.0.0.1. ` +
    `Pass --force if you are absolutely sure.`
  );
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `node --test safety.test.js`
Expected: PASS — all 5 tests green.

- [ ] **Step 5: Commit**

```bash
git add tools/traffic-bot/safety.js tools/traffic-bot/safety.test.js
git commit -m "traffic-bot: add localhost-only safety guard"
```

---

### Task 4: `stats.js` — latency/error aggregation and reporting

**Files:**
- Create: `tools/traffic-bot/stats.js`
- Test: `tools/traffic-bot/stats.test.js`

- [ ] **Step 1: Write the failing test**

```js
// stats.test.js
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { Stats } from './stats.js';

test('percentile of an empty series is null', () => {
  const stats = new Stats();
  const snap = stats.snapshot();
  assert.equal(snap.overall.count, 0);
  assert.equal(snap.overall.p50, null);
});

test('records success and error counts per endpoint', () => {
  const stats = new Stats();
  stats.record({ endpoint: 'POST /api/books/{id}/favorite', durationMs: 50, ok: true });
  stats.record({ endpoint: 'POST /api/books/{id}/favorite', durationMs: 80, ok: true });
  stats.record({ endpoint: 'POST /api/books/{id}/favorite', durationMs: 999, ok: false });
  const snap = stats.snapshot();
  const ep = snap.byEndpoint['POST /api/books/{id}/favorite'];
  assert.equal(ep.count, 3);
  assert.equal(ep.errors, 1);
  assert.equal(snap.overall.count, 3);
  assert.equal(snap.overall.errors, 1);
});

test('percentiles are computed from sorted durations', () => {
  const stats = new Stats();
  // 1..100ms, so p50=50-ish, p95=95-ish, p99=99-ish on a uniform series
  for (let ms = 1; ms <= 100; ms++) {
    stats.record({ endpoint: 'GET /api/books', durationMs: ms, ok: true });
  }
  const snap = stats.snapshot();
  const ep = snap.byEndpoint['GET /api/books'];
  assert.equal(ep.p50, 50);
  assert.equal(ep.p95, 95);
  assert.equal(ep.p99, 99);
});

test('reset clears all recorded data', () => {
  const stats = new Stats();
  stats.record({ endpoint: 'GET /api/books', durationMs: 10, ok: true });
  stats.reset();
  assert.equal(stats.snapshot().overall.count, 0);
});
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `node --test stats.test.js`
Expected: FAIL — `stats.js` does not exist yet.

- [ ] **Step 3: Write the implementation**

```js
// stats.js — pure in-memory aggregation, no I/O. index.js decides when to
// print a snapshot; this module only knows how to accumulate and summarize.

function percentile(sortedDurations, p) {
  if (sortedDurations.length === 0) return null;
  // Nearest-rank method: for [1..100] this yields sorted[49]=50 at p50,
  // sorted[94]=95 at p95, sorted[98]=99 at p99 — matches the test below.
  const idx = Math.max(0, Math.ceil((p / 100) * sortedDurations.length) - 1);
  return sortedDurations[idx];
}

export class Stats {
  constructor() {
    this.reset();
  }

  reset() {
    /** @type {Map<string, {durations: number[], errors: number}>} */
    this.byEndpoint = new Map();
    this.entitiesCreated = { users: 0, books: 0, orders: 0, payments: 0, reviews: 0 };
  }

  record({ endpoint, durationMs, ok }) {
    if (!this.byEndpoint.has(endpoint)) {
      this.byEndpoint.set(endpoint, { durations: [], errors: 0 });
    }
    const entry = this.byEndpoint.get(endpoint);
    entry.durations.push(durationMs);
    if (!ok) entry.errors += 1;
  }

  recordEntity(kind) {
    if (kind in this.entitiesCreated) this.entitiesCreated[kind] += 1;
  }

  snapshot() {
    const byEndpoint = {};
    let allDurations = [];
    let totalErrors = 0;

    for (const [endpoint, entry] of this.byEndpoint.entries()) {
      const sorted = [...entry.durations].sort((a, b) => a - b);
      byEndpoint[endpoint] = {
        count: sorted.length,
        errors: entry.errors,
        p50: percentile(sorted, 50),
        p95: percentile(sorted, 95),
        p99: percentile(sorted, 99),
      };
      allDurations = allDurations.concat(sorted);
      totalErrors += entry.errors;
    }

    allDurations.sort((a, b) => a - b);

    return {
      overall: {
        count: allDurations.length,
        errors: totalErrors,
        p50: percentile(allDurations, 50),
        p95: percentile(allDurations, 95),
        p99: percentile(allDurations, 99),
      },
      byEndpoint,
      entitiesCreated: { ...this.entitiesCreated },
    };
  }

  printSnapshot(label = 'Traffic snapshot') {
    const snap = this.snapshot();
    const errRate = snap.overall.count === 0
      ? '0.0'
      : ((snap.overall.errors / snap.overall.count) * 100).toFixed(1);
    console.log(
      `\n[${label}] requests=${snap.overall.count} errors=${snap.overall.errors} (${errRate}%) ` +
      `p50=${snap.overall.p50}ms p95=${snap.overall.p95}ms p99=${snap.overall.p99}ms`
    );
    for (const [endpoint, ep] of Object.entries(snap.byEndpoint)) {
      console.log(
        `  ${endpoint}: count=${ep.count} errors=${ep.errors} ` +
        `p50=${ep.p50}ms p95=${ep.p95}ms p99=${ep.p99}ms`
      );
    }
    console.log(
      `  entities created: users=${snap.entitiesCreated.users} books=${snap.entitiesCreated.books} ` +
      `orders=${snap.entitiesCreated.orders} payments=${snap.entitiesCreated.payments} reviews=${snap.entitiesCreated.reviews}`
    );
  }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `node --test stats.test.js`
Expected: PASS — all 4 tests green.

- [ ] **Step 5: Commit**

```bash
git add tools/traffic-bot/stats.js tools/traffic-bot/stats.test.js
git commit -m "traffic-bot: add latency/error stats aggregator"
```

---

### Task 5: `apiClient.js` — timed HTTP wrapper

**Files:**
- Create: `tools/traffic-bot/apiClient.js`

No unit test for this file — it's a thin axios wrapper whose only logic (timing + recording) is exercised for real once `journeys/*.js` calls it against the running backend in the end-to-end verification (Task 10). Mocking axios here would just test the mock.

- [ ] **Step 1: Write the implementation**

```js
// apiClient.js — every HTTP call the bot makes goes through here, so every
// call is timed and recorded into Stats uniformly. Journeys never call
// axios directly.

import axios from 'axios';

export class ApiClient {
  /**
   * @param {object} opts
   * @param {string} opts.baseURL
   * @param {import('./stats.js').Stats} opts.stats
   * @param {string} [opts.token] bearer token, if this client is authenticated
   */
  constructor({ baseURL, stats, token }) {
    this.stats = stats;
    this.token = token;
    this.http = axios.create({
      baseURL,
      timeout: 10000,
      validateStatus: () => true, // we record 4xx/5xx as data, not exceptions
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    });
  }

  /** Returns a new ApiClient sharing the same baseURL/stats but with a token. */
  withToken(token) {
    return new ApiClient({ baseURL: this.http.defaults.baseURL, stats: this.stats, token });
  }

  /**
   * @param {'GET'|'POST'|'PUT'|'DELETE'} method
   * @param {string} path e.g. '/api/books'
   * @param {string} statsLabel e.g. 'GET /api/books' (use {id} placeholders, not real IDs)
   * @param {object} [body]
   */
  async request(method, path, statsLabel, body) {
    const start = Date.now();
    let response;
    try {
      response = await this.http.request({ method, url: path, data: body });
    } catch (err) {
      // Network-level failure (connection refused, timeout) — not an HTTP status.
      this.stats.record({ endpoint: statsLabel, durationMs: Date.now() - start, ok: false });
      throw err;
    }
    const durationMs = Date.now() - start;
    const ok = response.status >= 200 && response.status < 300;
    this.stats.record({ endpoint: statsLabel, durationMs, ok });
    if (!ok) {
      const err = new Error(`${statsLabel} -> HTTP ${response.status}`);
      err.status = response.status;
      err.body = response.data;
      throw err;
    }
    return response.data;
  }

  async register({ username, fullName, email, password }) {
    return this.request('POST', '/api/auth/register', 'POST /api/auth/register', {
      username, fullName, email, password,
    });
  }

  async login({ email, password }) {
    return this.request('POST', '/api/auth/login', 'POST /api/auth/login', { email, password });
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add tools/traffic-bot/apiClient.js
git commit -m "traffic-bot: add timed API client wrapper"
```

---

### Task 6: `userPool.js` — growing, capped virtual user pool

**Files:**
- Create: `tools/traffic-bot/userPool.js`
- Test: `tools/traffic-bot/userPool.test.js`

- [ ] **Step 1: Write the failing test for the pure weighting decision**

```js
// userPool.test.js
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { shouldCreateNewUser } from './userPool.js';

test('always creates a new user when the pool is empty', () => {
  assert.equal(shouldCreateNewUser({ poolSize: 0, poolCap: 200, random: () => 0.99 }), true);
});

test('never creates a new user once the pool is at cap', () => {
  assert.equal(shouldCreateNewUser({ poolSize: 200, poolCap: 200, random: () => 0.0 }), false);
});

test('reuse likelihood increases as the pool fills', () => {
  // Half-full pool: with a fixed random draw, more of the pool being full
  // should only ever make "reuse" (false) more likely, never less.
  const drawsCreatedAt10Pct = shouldCreateNewUser({ poolSize: 20, poolCap: 200, random: () => 0.5 });
  const drawsCreatedAt90Pct = shouldCreateNewUser({ poolSize: 180, poolCap: 200, random: () => 0.5 });
  assert.equal(drawsCreatedAt90Pct <= drawsCreatedAt10Pct, true);
});
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `node --test userPool.test.js`
Expected: FAIL — `userPool.js` does not exist yet.

- [ ] **Step 3: Write the implementation**

```js
// userPool.js — manages virtual reader/vendor identities. Registration and
// login go through the ApiClient passed in; this module owns *which* user
// a worker should use next, not how HTTP calls are made.

import { faker } from '@faker-js/faker';

/**
 * Decide whether to register a brand-new user or reuse one from the pool.
 * Reuse probability rises linearly with how full the pool is, so early runs
 * grow the pool quickly and later runs are dominated by returning-user logins.
 * @param {{poolSize: number, poolCap: number, random?: () => number}} args
 */
export function shouldCreateNewUser({ poolSize, poolCap, random = Math.random }) {
  if (poolSize <= 0) return true;
  if (poolSize >= poolCap) return false;
  const fillRatio = poolSize / poolCap;
  // At fillRatio=0, always create (createChance=1). At fillRatio=1, never (createChance=0).
  const createChance = 1 - fillRatio;
  return random() < createChance;
}

export class UserPool {
  /**
   * @param {object} opts
   * @param {(baseUrl: string) => import('./apiClient.js').ApiClient} opts.makeClient
   * @param {string} opts.baseUrl
   * @param {number} opts.poolCap
   */
  constructor({ makeClient, baseUrl, poolCap }) {
    this.makeClient = makeClient;
    this.baseUrl = baseUrl;
    this.poolCap = poolCap;
    /** @type {{email: string, password: string, client: import('./apiClient.js').ApiClient}[]} */
    this.readers = [];
    /** @type {{email: string, password: string, client: import('./apiClient.js').ApiClient, applied: boolean}[]} */
    this.vendors = [];
  }

  // Registration always creates ROLE_USER server-side (see AuthController.register) —
  // vendor status comes later from admin approval, not from anything sent at registration.
  async _registerAndLogin(anonClient) {
    const password = 'TrafficBot123!';
    const email = faker.internet.email().toLowerCase();
    const username = faker.internet.username().toLowerCase().slice(0, 30);
    await anonClient.register({
      username,
      fullName: faker.person.fullName(),
      email,
      password,
    });
    const loginResp = await anonClient.login({ email, password });
    const token = loginResp.data.accessToken;
    return { email, password, client: anonClient.withToken(token) };
  }

  /** Returns an authenticated client for a reader (existing or new). */
  async getReaderClient() {
    const anon = this.makeClient(this.baseUrl);
    if (shouldCreateNewUser({ poolSize: this.readers.length, poolCap: this.poolCap })) {
      const user = await this._registerAndLogin(anon);
      this.readers.push(user);
      return user.client;
    }
    const existing = this.readers[Math.floor(Math.random() * this.readers.length)];
    const loginResp = await anon.login({ email: existing.email, password: existing.password });
    existing.client = anon.withToken(loginResp.data.accessToken);
    return existing.client;
  }

  /** Returns an authenticated client + vendor record for a vendor (existing or new). */
  async getVendorClient() {
    const anon = this.makeClient(this.baseUrl);
    if (shouldCreateNewUser({ poolSize: this.vendors.length, poolCap: Math.max(1, Math.floor(this.poolCap / 10)) })) {
      const user = await this._registerAndLogin(anon);
      // No vendorId field: admin.js approves using the pending application's own
      // id from GET /api/vendors/pending, not anything tracked here.
      const record = { ...user, applied: false };
      this.vendors.push(record);
      return record;
    }
    const existing = this.vendors[Math.floor(Math.random() * this.vendors.length)];
    const loginResp = await anon.login({ email: existing.email, password: existing.password });
    existing.client = anon.withToken(loginResp.data.accessToken);
    return existing;
  }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `node --test userPool.test.js`
Expected: PASS — all 3 tests green.

- [ ] **Step 5: Commit**

```bash
git add tools/traffic-bot/userPool.js tools/traffic-bot/userPool.test.js
git commit -m "traffic-bot: add virtual user pool with reuse-weighted growth"
```

---

## Chunk 2: Bootstrap, journeys, wiring, docs, verification

Tasks 7–12: the bootstrap step, the three journeys built on top of Chunk 1's plumbing, the entry point that wires everything together, usage docs, and end-to-end verification against the real running app.

### Task 7: `bootstrap.js` — ensure a subscription plan exists

**Files:**
- Create: `tools/traffic-bot/bootstrap.js`

- [ ] **Step 1: Write the implementation**

```js
// bootstrap.js — DataInitializer seeds users and books but no subscription
// plan, so GET /api/subscriptions/plans is empty on a fresh database. This
// runs once at startup (as the seeded admin) so the reader journey's
// subscribe step always has something to subscribe to.

export async function ensureSubscriptionPlan(adminClient) {
  const existing = await adminClient.request('GET', '/api/subscriptions/plans', 'GET /api/subscriptions/plans');
  if (existing.data && existing.data.length > 0) {
    return existing.data;
  }
  const plan = await adminClient.request(
    'POST',
    '/api/subscriptions/plans',
    'POST /api/subscriptions/plans',
    {
      name: 'Traffic Bot Standard Plan',
      planType: 'MONTHLY',
      price: 199.0,
      durationDays: 30,
      maxDownloadsPerMonth: 50,
    }
  );
  return [plan.data];
}
```

- [ ] **Step 2: Commit**

```bash
git add tools/traffic-bot/bootstrap.js
git commit -m "traffic-bot: seed a subscription plan if none exists"
```

Payload verified directly against `backend/src/main/java/com/digitallibrary/dto/CreateSubscriptionPlanRequest.java` — required fields are `name`, `planType`, `price`, `durationDays`, `maxDownloadsPerMonth` (there is no `description` field on this DTO).

---

### Task 8: `journeys/reader.js`

**Files:**
- Create: `tools/traffic-bot/journeys/reader.js`

- [ ] **Step 1: Write the implementation**

```js
// journeys/reader.js — the most common journey: browse, favorite, subscribe,
// buy, review. Every step is independent; a failure logs to stats (via
// apiClient, which already records it) and the journey moves on rather than
// aborting, since the goal is sustained traffic, not an all-or-nothing
// transaction.

import { faker } from '@faker-js/faker';

async function safeStep(fn) {
  try {
    await fn();
  } catch {
    // Already recorded in Stats by ApiClient; nothing else to do here.
  }
}

/**
 * @param {import('../apiClient.js').ApiClient} client authenticated as a reader
 * @param {import('../stats.js').Stats} stats
 * @param {number[]} planIds at least one subscription plan id to subscribe to
 */
export async function runReaderJourney(client, stats, planIds) {
  let books = [];
  await safeStep(async () => {
    const resp = await client.request('GET', '/api/books?page=0&size=10', 'GET /api/books');
    books = resp.data?.content ?? [];
  });

  if (books.length > 0) {
    const book = books[Math.floor(Math.random() * books.length)];

    await safeStep(() =>
      client.request('POST', `/api/books/${book.id}/favorite`, 'POST /api/books/{id}/favorite')
    );

    await safeStep(async () => {
      await client.request(
        'POST',
        `/api/payments/checkout`,
        'POST /api/payments/checkout',
        { bookIds: [book.id], paymentGateway: 'MOCK' }
      );
      stats.recordEntity('orders');
      stats.recordEntity('payments');
    });

    await safeStep(async () => {
      await client.request(
        'POST',
        `/api/books/${book.id}/reviews`,
        'POST /api/books/{id}/reviews',
        { rating: faker.number.int({ min: 3, max: 5 }), comment: faker.lorem.sentence() }
      );
      stats.recordEntity('reviews');
    });
  }

  if (planIds.length > 0) {
    const planId = planIds[Math.floor(Math.random() * planIds.length)];
    await safeStep(() =>
      client.request('POST', `/api/subscriptions/subscribe/${planId}`, 'POST /api/subscriptions/subscribe/{planId}')
    );
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add tools/traffic-bot/journeys/reader.js
git commit -m "traffic-bot: add reader journey"
```

---

### Task 9: `journeys/vendor.js` and `journeys/admin.js`

**Files:**
- Create: `tools/traffic-bot/journeys/vendor.js`
- Create: `tools/traffic-bot/journeys/admin.js`

- [ ] **Step 1: Write `journeys/vendor.js`**

```js
// journeys/vendor.js — apply once, then (once an admin has approved) publish
// books and check commissions. `vendorRecord` is the same object UserPool
// hands back each time this vendor is picked, so `applied` persists across
// loop iterations for that identity.

import { faker } from '@faker-js/faker';

async function safeStep(fn) {
  try {
    await fn();
  } catch {
    // Already recorded in Stats by ApiClient.
  }
}

/**
 * @param {{client: import('../apiClient.js').ApiClient, applied: boolean}} vendorRecord
 * @param {import('../stats.js').Stats} stats
 */
export async function runVendorJourney(vendorRecord, stats) {
  const { client } = vendorRecord;

  if (!vendorRecord.applied) {
    await safeStep(async () => {
      await client.request('POST', '/api/vendors/apply', 'POST /api/vendors/apply', {
        storeName: faker.company.name(),
        bio: faker.company.catchPhrase(),
      });
      vendorRecord.applied = true;
    });
    return; // nothing else to do this loop — wait for admin approval
  }

  // Publishing a book only succeeds once this identity holds ROLE_VENDOR
  // (granted by PUT /api/vendors/{id}/approve). Until then this 403s, gets
  // recorded as an error by ApiClient, and safeStep swallows it — the
  // journey simply retries on its next scheduled loop.
  await safeStep(async () => {
    await client.request('POST', '/api/books', 'POST /api/books', {
      title: faker.commerce.productName(),
      author: faker.person.fullName(),
      category: faker.helpers.arrayElement(['Programming', 'Cloud', 'Business', 'Fiction']),
      isbn: faker.string.alphanumeric(10).toUpperCase(),
      availableCopies: faker.number.int({ min: 1, max: 20 }),
      price: faker.number.float({ min: 99, max: 999, fractionDigits: 2 }),
      free: false,
    });
    stats.recordEntity('books');
  });

  await safeStep(() =>
    client.request('GET', '/api/vendors/me/commissions', 'GET /api/vendors/me/commissions')
  );
}
```

- [ ] **Step 2: Write `journeys/admin.js`**

```js
// journeys/admin.js — a periodic sweep, not a per-worker-loop journey like
// reader/vendor. Approves every pending vendor application so the vendor
// journey's second half (publishing books) is actually reachable, and spot-
// checks the audit log endpoint for traffic/coverage.

async function safeStep(fn) {
  try {
    await fn();
  } catch {
    // Already recorded in Stats by ApiClient.
  }
}

/** @param {import('../apiClient.js').ApiClient} adminClient */
export async function runAdminSweep(adminClient) {
  let pending = [];
  await safeStep(async () => {
    const resp = await adminClient.request('GET', '/api/vendors/pending', 'GET /api/vendors/pending');
    pending = resp.data?.content ?? resp.data ?? [];
  });

  for (const application of pending) {
    await safeStep(() =>
      adminClient.request(
        'PUT',
        `/api/vendors/${application.id}/approve`,
        'PUT /api/vendors/{id}/approve',
        {}
      )
    );
  }

  await safeStep(() =>
    adminClient.request('GET', '/api/admin/audit-logs', 'GET /api/admin/audit-logs')
  );
}
```

- [ ] **Step 3: Commit**

```bash
git add tools/traffic-bot/journeys/vendor.js tools/traffic-bot/journeys/admin.js
git commit -m "traffic-bot: add vendor journey and admin sweep"
```

Payload verified directly against `backend/src/main/java/com/digitallibrary/dto/VendorApplicationRequest.java` — the only fields are `storeName` (required) and `bio` (optional); there is no `organizationName` or `description` field on this DTO.

---

### Task 10: `index.js` — wire it all together

**Files:**
- Create: `tools/traffic-bot/index.js`

- [ ] **Step 1: Write the implementation**

```js
#!/usr/bin/env node
// index.js — entry point. Parses config, verifies the target is safe,
// bootstraps a subscription plan, then runs N concurrent workers until
// SIGINT. Worker 0 (when concurrency >= 2) is the dedicated admin-sweep
// worker; the rest run reader/vendor journeys.

import axios from 'axios';
import { loadConfig } from './config.js';
import { assertSafeTarget } from './safety.js';
import { Stats } from './stats.js';
import { ApiClient } from './apiClient.js';
import { UserPool } from './userPool.js';
import { ensureSubscriptionPlan } from './bootstrap.js';
import { runReaderJourney } from './journeys/reader.js';
import { runVendorJourney } from './journeys/vendor.js';
import { runAdminSweep } from './journeys/admin.js';

const READER_WEIGHT = 0.85; // vs vendor journey, for non-admin workers

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

// Like sleep(), but wakes up early (in <=200ms) once `running.value` flips to
// false, so Ctrl+C doesn't have to wait out a long interval (e.g. the 20s
// default admin sweep) before the final report can print.
async function interruptibleSleep(ms, running) {
  const step = 200;
  let waited = 0;
  while (waited < ms && running.value) {
    const chunk = Math.min(step, ms - waited);
    await sleep(chunk);
    waited += chunk;
  }
}

function randomDelay(config) {
  const span = config.maxDelayMs - config.minDelayMs;
  return config.minDelayMs + Math.floor(Math.random() * span);
}

async function waitForBackend(baseUrl, { retries = 10, delayMs = 2000 } = {}) {
  for (let attempt = 1; attempt <= retries; attempt++) {
    try {
      const resp = await axios.get(`${baseUrl}/api/health`, { timeout: 3000 });
      if (resp.status === 200) return;
    } catch {
      // fall through to retry
    }
    console.log(`Backend not ready yet (attempt ${attempt}/${retries}), retrying in ${delayMs}ms...`);
    await sleep(delayMs);
  }
  throw new Error(`Backend at ${baseUrl} did not become ready — is it running?`);
}

async function runReaderOrVendorWorker(config, stats, pool, planIds, running) {
  while (running.value) {
    if (Math.random() < READER_WEIGHT) {
      const client = await pool.getReaderClient().catch(() => null);
      if (client) await runReaderJourney(client, stats, planIds);
    } else {
      const vendorRecord = await pool.getVendorClient().catch(() => null);
      if (vendorRecord) await runVendorJourney(vendorRecord, stats);
    }
    await interruptibleSleep(randomDelay(config), running);
  }
}

async function runAdminWorker(config, adminClient, running) {
  while (running.value) {
    await runAdminSweep(adminClient);
    await interruptibleSleep(config.adminSweepIntervalMs, running);
  }
}

async function main() {
  const config = loadConfig();
  assertSafeTarget(config);

  console.log(`Traffic bot starting against ${config.targetUrl} (concurrency=${config.concurrency})`);
  await waitForBackend(config.targetUrl);

  const stats = new Stats();
  const makeClient = (baseUrl) => new ApiClient({ baseURL: baseUrl, stats });
  const pool = new UserPool({ makeClient, baseUrl: config.targetUrl, poolCap: config.userPoolCap });

  const anon = makeClient(config.targetUrl);
  const adminLogin = await anon.login({ email: config.adminEmail, password: config.adminPassword });
  const adminClient = anon.withToken(adminLogin.data.accessToken);

  const plans = await ensureSubscriptionPlan(adminClient);
  const planIds = plans.map((p) => p.id);

  const running = { value: true };
  const workers = [];

  if (config.concurrency >= 2) {
    workers.push(runAdminWorker(config, adminClient, running));
    for (let i = 1; i < config.concurrency; i++) {
      workers.push(runReaderOrVendorWorker(config, stats, pool, planIds, running));
    }
  } else {
    workers.push(runReaderOrVendorWorker(config, stats, pool, planIds, running));
  }

  const statsTimer = setInterval(() => stats.printSnapshot('Traffic snapshot'), config.statsIntervalMs);

  const shutdown = () => {
    if (!running.value) return; // already shutting down
    console.log('\nStopping — letting in-flight requests finish...');
    running.value = false;
    clearInterval(statsTimer);
  };
  process.on('SIGINT', shutdown);

  await Promise.all(workers);
  stats.printSnapshot('Final report');
  process.exit(0);
}

main().catch((err) => {
  console.error('Traffic bot failed to start:', err.message);
  process.exit(1);
});
```

- [ ] **Step 2: Commit**

```bash
git add tools/traffic-bot/index.js
git commit -m "traffic-bot: wire up entry point with workers and graceful shutdown"
```

---

### Task 11: `README.md` for the tool

**Files:**
- Create: `tools/traffic-bot/README.md`

- [ ] **Step 1: Write the file**

```markdown
# Digital Library Traffic Bot

A local-only dev tool. It drives the running Digital Library app through
realistic reader/vendor/admin journeys — registering, browsing, subscribing,
paying, reviewing, publishing, approving — until you stop it, so you can see
the app behave under real traffic: data accumulating, requests flowing,
latency and errors visible live.

**It refuses to run against anything but `localhost`/`127.0.0.1`.** See
`safety.js` if you ever need to understand or override that (`--force`,
not recommended).

## Prerequisites

The full local stack from the main repo README must already be running:
Postgres + Redis (`docker compose up -d postgres redis`), the backend
(`mvn spring-boot:run -Dspring-boot.run.profiles=local`), on the default
`http://localhost:8000`.

## Run it

```powershell
cd tools/traffic-bot
npm install   # first time only
npm start
```

Press `Ctrl+C` to stop — it finishes in-flight requests and prints a final
report before exiting.

## Configuration (environment variables)

| Variable | Default | Meaning |
|---|---|---|
| `TRAFFIC_BOT_TARGET_URL` | `http://localhost:8000` | Backend base URL. Must be localhost/127.0.0.1 unless `--force` is passed. |
| `TRAFFIC_BOT_CONCURRENCY` | `8` | Number of concurrent workers. One is dedicated to the admin sweep when this is >= 2. |
| `TRAFFIC_BOT_POOL_CAP` | `200` | Max number of virtual reader users. Growth stops here; traffic keeps going via returning-user logins. |
| `TRAFFIC_BOT_MIN_DELAY_MS` / `TRAFFIC_BOT_MAX_DELAY_MS` | `300` / `1500` | Random pause range between a worker's journey steps. |
| `TRAFFIC_BOT_ADMIN_SWEEP_MS` | `20000` | How often the admin worker checks for pending vendor applications. |
| `TRAFFIC_BOT_STATS_INTERVAL_MS` | `10000` | How often a live stats snapshot prints to the console. |
| `TRAFFIC_BOT_ADMIN_EMAIL` / `TRAFFIC_BOT_ADMIN_PASSWORD` | `admin@library.com` / `admin123` | Must match a seeded admin account (`DataInitializer`). |

## What it does *not* do

- Doesn't run against production or any non-local target (by design).
- Isn't a correctness test suite — it doesn't assert expected outcomes.
- Isn't a raw stress-test tool — pacing is deliberately realistic, not "as fast as possible."
```

- [ ] **Step 2: Commit**

```bash
git add tools/traffic-bot/README.md
git commit -m "traffic-bot: add usage documentation"
```

---

### Task 12: Manual end-to-end verification

**Files:** none (verification only)

- [ ] **Step 1: Confirm the local stack is running**

Run: `curl -s http://localhost:8000/api/health`
Expected: `{"success":true,...,"data":{"backend":"UP","database":"UP","redis":"UP"},...}`

If not, bring it up first: `docker compose up -d postgres redis`, then `mvn spring-boot:run -Dspring-boot.run.profiles=local` from `backend/`, per the main repo README.

- [ ] **Step 2: Run all unit tests**

Run (from `tools/traffic-bot/`): `npm test`
Expected: all `safety.test.js`, `stats.test.js`, `userPool.test.js` tests pass.

- [ ] **Step 3: Start the bot and watch the first snapshot**

Run: `npm start`
Expected within ~10s: a `[Traffic snapshot]` line with a non-zero request count, an error rate, and per-endpoint latency. If `bootstrap.js`'s plan-creation or a journey's DTO fields are wrong (see the notes in Tasks 7 and 9), this is where a validation-error spike in the stats output will surface it — fix the payload field names against the actual DTOs and restart.

- [ ] **Step 4: Confirm data is actually accumulating**

In a second terminal, run: `curl -s "http://localhost:8000/api/books?page=0&size=50" | grep -o '"id"' | wc -l`
Expected: the count grows if you run this again a minute later (vendor-published books joining the original 4 seeded ones).

- [ ] **Step 5: Confirm the safety guard**

Run: `TRAFFIC_BOT_TARGET_URL=http://example.com npm start`
Expected: immediate refusal — `Traffic bot failed to start: Refusing to run: target "http://example.com" is not localhost/127.0.0.1...` (the `Traffic bot failed to start:` prefix comes from `main().catch()` in `index.js`, which is what actually prints the error) — process exits without sending any traffic.

- [ ] **Step 6: Stop it cleanly**

Press `Ctrl+C` in the terminal running `npm start`.
Expected: "Stopping — letting in-flight requests finish..." followed by a `[Final report]` snapshot, then the process exits.

- [ ] **Step 7: Commit the verification note (if any fixes were needed in Steps 3–4)**

If Task 7 or Task 9's DTO field names needed correcting, commit that fix now:
```bash
git add tools/traffic-bot/bootstrap.js tools/traffic-bot/journeys/vendor.js
git commit -m "traffic-bot: fix DTO field names found during end-to-end verification"
```
