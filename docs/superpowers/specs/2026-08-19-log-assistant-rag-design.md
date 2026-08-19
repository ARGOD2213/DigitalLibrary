# Log Assistant RAG — v1 Design

**Status:** Approved for planning
**Date:** 2026-08-19
**Scope:** First of three planned RAG sub-projects for the Digital Library platform (log/ops failure assistant, payments analysis, S3 cost optimization). Only the log/ops assistant is designed here; the other two are out of scope for this spec.

## Problem

The team has no way to ask "what went wrong" or "why was the API slow" without manually grepping `backend-dev.out.log` / `backend-dev.err.log` (or, in production, SSH-ing into the EC2 box and reading `docker logs`). This spec covers a retrieval-augmented assistant that answers natural-language questions over the backend's logs, admin-only, inside the existing React frontend.

A future phase (not built here) adds a proactive watcher that detects anomalies (error spikes, latency jumps) without being asked, and alerts via the app's existing SES/SNS notification service.

## Non-goals (v1)

- No vector embeddings / semantic similarity search. Logs are structured and timestamped; time-range + level + keyword filtering is retrieval enough for launch. pgvector is the noted upgrade path if log volume or query sophistication grows.
- No production/CloudWatch log ingestion. Local dev logs (`backend-dev.out.log`, `backend-dev.err.log`) only.
- No proactive alerting/anomaly detection (phase 2).
- No Slack or other external integrations. UI lives in the existing React admin panel.
- Redis is not used as a vector store. The `redis:7-alpine` image in `docker-compose.yml` has no RediSearch module, and mixing a durable log index into the same store used for JWT blacklists and rate-limit counters is the wrong fit. Redis's role here is limited to caching the recent-log hot window and recent answers, matching how it's already used elsewhere in this app (Spring Cache).

## Architecture

```
backend-dev.out.log / .err.log
        |
        v
  LogTailerService (tails files, parses lines, handles multi-line stack traces)
        |
        v
  log_entries table (Postgres, via Flyway migration V2)
        |
        v
  LogAssistantService (derives filter from question -> queries log_entries
                        -> caches hot window + recent answers in Redis)
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

### 1. Ingestion — `LogTailerService`

- New package: `com.digitallibrary.logassistant`.
- Runs as a background component (Spring `@Scheduled` poll, e.g. every 5s — not a raw thread, to stay consistent with the existing `SubscriptionExpiryScheduler` pattern) that tails both log files from a persisted byte offset.
- Parses each line using the default Spring Boot log pattern (`timestamp level pid --- [thread] logger : message`). Lines that don't match the pattern (stack trace continuation lines) are appended to the previous entry's `message`.
- Detects log rotation/truncation by checking if file size has decreased since last read; on truncation, resets the offset to 0.
- Batches inserts to `log_entries` (e.g. every poll cycle, capped batch size) rather than one INSERT per line.

### 2. Storage — `log_entries` table

New Flyway migration `V2__create_log_entries.sql`:

```sql
CREATE TABLE log_entries (
    id BIGSERIAL PRIMARY KEY,
    source VARCHAR(20) NOT NULL,        -- 'stdout' | 'stderr'
    logged_at TIMESTAMP NOT NULL,
    level VARCHAR(10) NOT NULL,         -- INFO, WARN, ERROR, DEBUG
    logger VARCHAR(255),
    message TEXT NOT NULL,
    raw_line TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_log_entries_logged_at ON log_entries (logged_at);
CREATE INDEX idx_log_entries_level ON log_entries (level);
```

A scheduled cleanup job (same scheduler package) prunes rows older than a configurable retention window (default 14 days) so the table doesn't grow unbounded on a dev machine.

### 3. Retrieval — `LogAssistantService`

- Input: a natural-language question.
- v1 filter derivation: simple rule-based extraction (keywords like "today", "last hour", "errors", "slow" map to time-range/level filters). No LLM call needed for this step in v1 — keeps it cheap and deterministic. If this proves too limited, upgrading to LLM-assisted structured-filter extraction (tool-use/function-calling) is a follow-up, not a blocker for v1.
- Queries `log_entries` with the derived filter, capped to a token-safe number of rows (e.g. top 50 most relevant by recency within the filtered set).
- Redis usage:
  - Caches the last N minutes of parsed entries (the "hot window") so repeated questions about recent activity don't re-hit Postgres.
  - Caches recent question → answer pairs (keyed by normalized question + time bucket) to avoid redundant LLM calls for repeated questions.
- Builds a prompt containing the matched log lines (with timestamps) and the user's question, calls `LlmClient`, returns the answer plus the specific log entries it cited.

### 4. LLM integration — `LlmClient` interface

- `LlmClient.ask(String systemContext, String question) -> String`.
- `AnthropicLlmClient` implementation (v1): calls the Claude API (Haiku model by default — this is log summarization, not deep reasoning) using an API key from config.
- New config: `LOG_ASSISTANT_LLM_PROVIDER=anthropic`, `ANTHROPIC_API_KEY=`, `LOG_ASSISTANT_MODEL=` (default to a Haiku model id), added to `.env.example` alongside the existing AWS/Redis config blocks.
- The interface boundary means a future local/offline model (e.g. via Ollama) is a new `LlmClient` implementation, not a rewrite — matches the user's stated preference to use an API now and revisit offline later.

### 5. API — `AdminLogAssistantController`

- `POST /api/admin/log-assistant/ask` — body `{ "question": string }`, response `{ "answer": string, "citedEntries": [{ id, loggedAt, level, message }] }`.
- `ROLE_ADMIN` only, consistent with `/api/admin/audit-logs`.
- Rate-limited via the existing Bucket4j `RateLimitFilter` — no new rate-limit infra needed.

### 6. Frontend — Log Assistant admin page

- New page in the React admin section, alongside the existing audit log view.
- Chat-style input/response list; each answer displays its cited log lines (timestamp + level + message) so the admin can verify the assistant's claim against the source.

## Error handling

- **LLM call fails or times out**: fall back to returning the raw matched log entries with a note that summarization was unavailable, rather than a hard error.
- **Log file rotated/truncated**: detected via file-size check; tailer offset resets to 0 and resumes.
- **No log entries match the derived filter**: return a clear "no matching log entries found" response instead of calling the LLM with empty context.
- **Redis unavailable**: cache reads/writes fail open (service falls back to querying Postgres directly) — matches how the rest of the app already treats Redis as a cache, not a source of truth.

## Testing

- Unit tests for the log line parser, including multi-line stack trace continuation and malformed lines.
- Unit tests for filter derivation (question → time range/level/keyword filter) covering the supported keyword patterns.
- Integration test for `POST /api/admin/log-assistant/ask` with a mocked `LlmClient`, covering: happy path, no-matches path, LLM-failure fallback path.
- Manual verification: run the backend locally, generate some ERROR/WARN log lines, confirm the admin page surfaces them correctly with accurate citations.

## Future work (explicitly out of scope here)

- Phase 2: `LogAnomalyScheduler` (same shape as `SubscriptionExpiryScheduler`) computing a rolling error-rate baseline from `log_entries`, alerting via the existing AWS SES/SNS service on spikes.
- CloudWatch/production log ingestion as a second `LogTailerService` source.
- pgvector-based semantic search if keyword/time filtering proves insufficient.
- The payments-analysis RAG and S3 cost-optimization RAG (separate specs, separate sub-projects).
