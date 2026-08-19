# Log Assistant RAG — v1 Design

**Status:** Approved for planning
**Date:** 2026-08-19
**Scope:** First of three planned RAG sub-projects for the Digital Library platform (log/ops failure assistant, payments analysis, S3 cost optimization). Only the log/ops assistant is designed here; the other two are out of scope for this spec.

## Problem

The team has no way to ask "what errors happened today" or "why did checkout fail" without manually grepping log output (or, in production, SSH-ing into the EC2 box and reading `docker logs`). This spec covers a retrieval-augmented assistant that answers natural-language questions over the backend's logs, admin-only, inside the existing React frontend.

**Explicitly not answerable in v1: "why was the API slow."** Nothing in the codebase currently logs request duration (confirmed: no timing/latency logging anywhere in `backend/src/main/java`), so there is no data for a slowness question to retrieve against. Latency instrumentation is called out as required future work (see Future work) — this design covers error/event questions only, where the underlying log data actually exists today.

Note: the `backend-dev.out.log` / `backend-dev.err.log` files present in the repo today are leftovers from an ad-hoc manual run (`mvn spring-boot:run > backend-dev.out.log 2> backend-dev.err.log`, most likely) — there is no `logging.file.name` or logback config in `application.properties`, no dev-run script, and nothing in `docker-compose.yml` that produces them reliably. This spec includes adding real file-logging config as a prerequisite (see Component 1), rather than assuming those files already exist in a dependable way.

A future phase (not built here) adds a proactive watcher that detects anomalies (error spikes, latency jumps) without being asked, and alerts via the app's existing SES/SNS notification service.

## Non-goals (v1)

- No vector embeddings / semantic similarity search. Logs are structured and timestamped; time-range + level + keyword filtering is retrieval enough for launch. pgvector is the noted upgrade path if log volume or query sophistication grows.
- No production/CloudWatch log ingestion. The local dev log file (see Component 1) only.
- No proactive alerting/anomaly detection (phase 2).
- No request-latency instrumentation. "Slowness" questions aren't answerable until duration logging exists — see Future work.
- No Slack or other external integrations. UI lives in the existing React admin panel.
- Redis is not used as a vector store. The `redis:7-alpine` image in `docker-compose.yml` has no RediSearch module needed for vector search, and this app's only existing Redis usage (`@Cacheable` subscription-plan lookups in `SubscriptionServiceImpl`) is a plain query cache, not a durable index — introducing vector-search infrastructure there would be a new, heavier pattern this app doesn't have anywhere else. Redis's role here is limited to caching the recent-log hot window and recent answers: cache, not source of truth, same philosophy as the existing `@Cacheable` usage even though the new caching (raw sorted-set/TTL manipulation via `RedisTemplate`) is a different mechanism than declarative `@Cacheable`.

## Architecture

Data flow (top to bottom), not a call/dependency chain — the controller is the actual entry point and invokes the service, which in turn uses the tailer's stored data:

```
logging.file.name (new logback config, see Component 1)
        |
        v
  LogTailerService (tails the file, parses lines, handles multi-line stack traces)
        |
        v
  log_entries table (Postgres, via Flyway migration V2)
        |
        v
  LogAssistantService (derives filter from question -> queries log_entries
                        -> caches hot window + recent answers in Redis)
        |
        v
  LogRedactor (masks emails / order / transaction IDs before anything
               leaves the backend)
        |
        v
  LlmClient (interface; AnthropicLlmClient implementation calls Claude)
        |
        v
  AdminLogAssistantController  --  POST /api/admin/log-assistant/ask
        |
        v
  React admin page: "Log Assistant" (chat-style, cites source log lines)
```

## Components

### 1. Log production — `application.properties`

Before there's anything to tail, the backend needs to reliably produce a log file. Add to `backend/src/main/resources/application.properties`:

```properties
logging.file.name=logs/digital-library-backend.log
```

This turns on Spring Boot's default logback rolling-file appender (in addition to the existing console output — nothing about current dev workflow changes): all log levels the app already emits (INFO/WARN/ERROR/DEBUG, from any logger) go to this one file at a known path, with real size/date-based rotation and archiving handled by logback's default policy. This replaces the stale `backend-dev.out.log`/`backend-dev.err.log` files as the ingestion source — a single file is sufficient since `level` is already parsed per-line, so there's no need to separate stdout/stderr into two files. The `logs/` directory should be added to `.gitignore` alongside the existing dev log files.

### 2. Ingestion — `LogTailerService`

- New package: `com.digitallibrary.logassistant`.
- Runs as a background component (Spring `@Scheduled` poll, e.g. every 5s — not a raw thread, to stay consistent with the existing `SubscriptionExpiryScheduler` pattern) that tails `logs/digital-library-backend.log` from a persisted byte offset.
- The offset is persisted in a new `log_tailer_state` table (single row: `file_path`, `byte_offset`, `updated_at`), created in the same `V2` migration as `log_entries`. On the very first poll after startup, if no row exists yet, the service inserts one (`file_path=logs/digital-library-backend.log, byte_offset=0`) before reading — an upsert, not an assumption that the row is pre-seeded. Each poll cycle thereafter updates the offset in the same transaction as its `log_entries` batch insert, so a crash mid-cycle re-reads (and re-inserts) at most one batch rather than the whole file — duplicate rows from that edge case are an accepted v1 tradeoff, not silently unhandled.
- Parses each line using the default Spring Boot log pattern (`timestamp level pid --- [thread] logger : message`). Lines that don't match the pattern (stack trace continuation lines) are appended to the previous entry's `message`.
- Detects log rotation by checking if file size has decreased since last read (a real, well-defined event now that logback's rolling policy — not shell redirection — owns rotation); on rotation, resets the offset to 0 (in both the in-memory reader and `log_tailer_state`). Rotated/archived files are not backfilled in v1 — only the active file is tailed.
- Batches inserts to `log_entries` — up to 500 lines per poll cycle — rather than one INSERT per line.

### 3. Storage — `log_entries` table

New Flyway migration `V2__create_log_entries.sql`:

```sql
CREATE TABLE log_entries (
    id BIGSERIAL PRIMARY KEY,
    logged_at TIMESTAMP NOT NULL,
    level VARCHAR(10) NOT NULL,         -- INFO, WARN, ERROR, DEBUG
    logger VARCHAR(255),
    message TEXT NOT NULL,
    raw_line TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_log_entries_logged_at ON log_entries (logged_at);
CREATE INDEX idx_log_entries_level ON log_entries (level);

CREATE TABLE log_tailer_state (
    id SMALLINT PRIMARY KEY DEFAULT 1,
    file_path VARCHAR(500) NOT NULL,
    byte_offset BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT single_row CHECK (id = 1)
);
```

Keyword filtering (e.g. `ILIKE '%keyword%'` over `message`) is a supported query type, so a `pg_trgm` GIN index on `message` is included here rather than left as a later fix:

```sql
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX idx_log_entries_message_trgm ON log_entries USING GIN (message gin_trgm_ops);
```

A scheduled cleanup job (same scheduler package) prunes rows older than a configurable retention window (default 14 days) so the table doesn't grow unbounded on a dev machine.

### 4. Retrieval — `LogAssistantService`

- Input: a natural-language question.
- v1 filter derivation: simple rule-based extraction (keywords like "today", "last hour", "errors", "warnings" map to time-range/level filters — no "slow"/latency keyword, since that data doesn't exist; see Problem). Filters resolve to an explicit `[from, to]` timestamp range; if no time keyword is matched, default to the last 6 hours (not the full table) so an unrecognized question doesn't trigger a full scan. No LLM call needed for this step in v1 — keeps it cheap and deterministic. If this proves too limited, upgrading to LLM-assisted structured-filter extraction (tool-use/function-calling) is a follow-up, not a blocker for v1.
- Queries `log_entries` with the derived filter, capped to a token-safe number of rows (top 50 most relevant by recency within the filtered set).
- Redis usage:
  - **Hot-window cache**: `LogTailerService` writes each poll batch's parsed entries into a Redis sorted set (key `log:hotwindow`, scored by epoch millis of `logged_at`; member is the JSON-serialized entry **including its Postgres `id`**, so two entries with identical text at the same millisecond don't collide and silently collapse into one), trimming entries older than 15 minutes on every write (write-through — the tailer is the sole writer). The 20-minute Redis key TTL is refreshed (`EXPIRE`) on every write, not just at key creation, so it only lapses if the tailer actually stops running, not on a fixed clock while it's healthy. `LogAssistantService` reads from this sorted set instead of Postgres whenever the derived filter's entire `[from, to]` range falls within the last 15 minutes **and the key exists**; if the key is missing (tailer stopped, TTL lapsed) or the range reaches further back, it queries Postgres directly instead of treating a missing key as "no entries." The same level/keyword filtering and top-50-by-recency cap that apply to the Postgres path are applied client-side after `ZRANGEBYSCORE` on this path too — the hot window is a faster source for the same query, not a different, unfiltered/uncapped one.
  - **Q&A cache**: caches the full response object (`answer` + `citedEntries`, i.e. exactly what the endpoint returns) — not just the answer string — keyed by normalized question text + the resolved `[from, to]` rounded to the nearest 5-minute bucket, with a 10-minute TTL. This is populated **only when the derived filter's `to` timestamp is more than 5 minutes in the past** (i.e. a genuinely historical, immutable query window). Questions whose filter reaches up to "now" (the tool's primary live-triage use case) always skip this cache and query fresh, so a new ERROR is reflected immediately rather than hidden behind a stale cached answer.
- **Redaction (`LogRedactor`) before the prompt is built**: this app's own logs already contain PII (e.g. `PaymentServiceImpl` logs the user's email, order number, and transaction ID on checkout). A `LogRedactor` step runs over every matched line before it is included in the LLM prompt, masking email addresses and known identifier patterns (order/transaction IDs) with placeholders (e.g. `user@***`, `[ORDER_ID]`). This is a required step, not optional — matched lines are sent to a third-party API (Claude) and must not carry raw PII off-platform. The **citedEntries returned to the frontend also carry the redacted message**, not the raw one, so the admin UI never displays unredacted PII either.
- Builds a prompt containing all redacted, matched log lines (with timestamps) and the user's question, calls `LlmClient`, and returns the answer alongside those same matched entries as `citedEntries`. **`citedEntries` means "the context rows the answer was generated from," not a model-selected citation subset** — `LlmClient.ask` returns a plain string (see Component 5) with no mechanism for the model to indicate which specific rows it drew on, so the API always returns the full context set it was given. This is also why the LLM-unavailable fallback path can reuse the same field/shape to hold the raw matched entries directly.

### 5. LLM integration — `LlmClient` interface

- `LlmClient.ask(String systemContext, String question) -> String`.
- `AnthropicLlmClient` is the only implementation in v1, and is wired as the single `LlmClient` Spring bean directly — no provider-selection config, since there is nothing yet to select between. When a second implementation (e.g. a local/offline model via Ollama) is added later, that's the point at which a provider-switch mechanism (conditional `@Bean`, config property, etc.) gets designed — not before.
- This app has never made an outbound third-party HTTP call before (no `RestTemplate`/`WebClient` usage anywhere in the codebase, no webflux dependency). `AnthropicLlmClient` uses JDK 17's built-in `java.net.http.HttpClient` rather than adding a new HTTP dependency.
- New config: `ANTHROPIC_API_KEY=`, `LOG_ASSISTANT_MODEL=` (default `claude-haiku-4-5`), added to `.env.example` alongside the existing AWS/Redis config blocks.
- The interface boundary (`LlmClient`) is what makes a future local/offline model a new implementation rather than a rewrite — matches the user's stated preference to use an API now and revisit offline later.

### 6. API — `AdminLogAssistantController`

- `POST /api/admin/log-assistant/ask` — body `{ "question": string }`, response `{ "answer": string, "citedEntries": [{ id, loggedAt, level, message }] }`.
- `question` is validated as non-blank with a max length (e.g. 500 chars) via `@Valid`/Bean Validation, consistent with existing request DTOs — rejected with the app's standard validation error response, not passed to the LLM.
- `ROLE_ADMIN` only, consistent with `/api/admin/audit-logs`.
- Rate-limited via the existing Bucket4j `RateLimitFilter` (blanket 60 req/min/IP, shared across the whole API) — no new rate-limit infra needed for v1. Since LLM calls carry real per-call cost, a stricter per-endpoint limit is a noted follow-up if usage patterns warrant it, not a v1 blocker.

### 7. Frontend — Log Assistant admin page

- New route/page in the React admin section (e.g. `/admin/log-assistant`), linked from the same admin nav area as the existing audit log page.
- Chat-style input/response list; each answer displays its cited log lines (timestamp + level + redacted message) in a collapsed/expandable block (up to 50 lines is too much to always render inline) so the admin can verify the assistant's claim against the source.
- Loading state: input disabled and a spinner/placeholder shown in the response list while the request is in flight.
- The two backend fallback responses each get distinct rendering, not treated as errors:
  - **No matching log entries**: response list shows a plain "No log entries matched that question" message, no citations block.
  - **LLM unavailable (fallback path)**: response list shows the raw matched log entries directly (same citation format as a normal answer) with a small inline note that summarization was unavailable, rather than a generated answer.
- A true request failure (network error, 5xx, validation rejection) shows a standard inline error message and re-enables the input.

## Error handling

- **LLM call fails or times out**: fall back to returning the raw matched log entries with a note that summarization was unavailable, rather than a hard error.
- **Log file rotated**: logback's rolling policy (from the `logging.file.name` config in Component 1) renames the active file when it exceeds its size threshold and starts a fresh one at the same path; detected via file-size check, tailer offset resets to 0 and resumes on the new file. Already-rotated/archived files are not backfilled.
- **No log entries match the derived filter**: return a clear "no matching log entries found" response instead of calling the LLM with empty context.
- **Redis unavailable**: cache reads/writes fail open (service falls back to querying Postgres directly) — matches how the rest of the app already treats Redis as a cache, not a source of truth.

## Testing

- Unit tests for the log line parser, including multi-line stack trace continuation and malformed lines.
- Unit tests for filter derivation (question → time range/level/keyword filter) covering the supported keyword patterns.
- Unit tests for `LogRedactor`, covering email addresses and order/transaction ID patterns pulled from the app's actual log statements (e.g. the `PaymentServiceImpl` checkout log).
- Unit tests for log rotation/truncation detection in `LogTailerService` (file size decreases → offset resets to 0, both in-memory and in `log_tailer_state`), and for first-poll `log_tailer_state` seeding when no row exists yet.
- Unit tests for the hot-window cache: entries older than 15 minutes are trimmed on write, and `LogAssistantService` routes a query to Redis vs. Postgres correctly based on whether the filter's range falls entirely within the hot window.
- Integration test for `POST /api/admin/log-assistant/ask` with a mocked `LlmClient`, covering all four error-handling scenarios: happy path, no-matches path, LLM-failure fallback path, and Redis-unavailable fail-open path (cache calls fail, service still answers correctly from Postgres).
- Manual verification: run the backend locally, generate some ERROR/WARN log lines (including one with an email/order ID), confirm the admin page surfaces them correctly with accurate, redacted citations.

## Future work (explicitly out of scope here)

- Request-latency instrumentation (a timing filter/aspect logging request duration) so "why was it slow" questions become answerable — a prerequisite for both this assistant's slowness queries and the phase-2 anomaly watcher's latency-jump detection below.
- Phase 2: `LogAnomalyScheduler` (same shape as `SubscriptionExpiryScheduler`) computing a rolling error-rate baseline from `log_entries`, alerting via the existing AWS SES/SNS service on spikes.
- CloudWatch/production log ingestion as a second `LogTailerService` source.
- pgvector-based semantic search if keyword/time filtering proves insufficient.
- The payments-analysis RAG and S3 cost-optimization RAG (separate specs, separate sub-projects).
