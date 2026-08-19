# Spring AI Integration — v1 Design

**Status:** Approved for planning
**Date:** 2026-08-20
**Scope:** Infrastructure only — get Spring AI genuinely wired up and provably working against a real (free-tier) model. No RAG, no log ingestion, no persistence. This is the foundation the already-approved [Log Assistant RAG spec](2026-08-19-log-assistant-rag-design.md) can build on next, using Spring AI's `ChatClient` instead of that spec's originally-planned hand-rolled `java.net.http.HttpClient`.

## Problem

The project has no AI integration at all today. The user wants to start building AI features (starting with the already-designed log assistant, and eventually other RAG work) on top of Spring's own AI framework rather than hand-rolled HTTP clients, so future features get Spring AI's provider abstraction, prompt templating, and ecosystem for free. Before any feature work happens, the project needs: a compatible Spring Boot version, the Spring AI dependency wired up, a configured model provider, and one concrete proof that a real prompt sent through Spring's `ChatClient` gets a real answer back.

## Non-goals

- No RAG, no vector store, no document ingestion — that's the log-assistant spec's job (or a future spec), not this one.
- No persistence of prompts/answers — this is a stateless smoke test, not a chat history feature.
- No Spring Boot 4 / Spring AI 2.0 migration. Spring AI 2.0 requires Spring Boot 4.0/4.1 and Spring Framework 7, which would mean working through breaking changes across this project's existing Security config, JPA usage, and every other Spring module first — a separate, much larger migration project explicitly out of scope here. This spec targets Spring AI's 1.1.x line, which supports Spring Boot 3.5.x — the smallest version bump that unlocks Spring AI at all.
- No provider abstraction/multi-provider support in the code. Spring AI's `ChatModel` interface already gives an easy swap path later (e.g., to Anthropic Claude, which the log-assistant spec originally assumed) without this spec needing to build anything extra for it.
- Not wiring AI into any existing feature (payments, books, etc.) — this is a standalone, isolated slice.

## Version and dependency changes

- **Spring Boot parent**: `3.2.5` → the latest available `3.5.x` patch at implementation time (exact patch not pinned here since it will have moved on by execution; verify against Maven Central rather than assuming a specific number). This is a same-major-line bump — low risk relative to a 3.x→4.x jump, but the implementer should still run the full existing test/build cycle after the bump alone, before adding anything Spring AI–related, to isolate any breakage the version bump itself causes from anything this spec adds.
- **Spring AI BOM**: add `org.springframework.ai:spring-ai-bom` (latest `1.1.x` patch) to `backend/pom.xml`'s existing `<dependencyManagement>` block, alongside the AWS SDK BOM already there.
- **New dependency**: `org.springframework.ai:spring-ai-starter-model-google-genai` — Spring Boot auto-configuration for Google's GenAI Chat Client, added to `<dependencies>`.

## Provider configuration — and a real gotcha to guard against

The `google-genai` starter supports **two** authentication modes behind one dependency:
- **Gemini Developer API** (what this spec uses): a plain API key, free rate-limited tier, no GCP billing account.
- **Vertex AI**: GCP project credentials, billing-account-backed, paid.

**The switch between them is implicit and dangerous**: if a `project-id` or `location` property is set *anywhere* in Spring config — even one left over from a copy-pasted example, even set to an empty/placeholder value — the client silently switches to Vertex AI mode and rejects the free API key with a 400 auth error. There is no explicit "mode" flag; the presence of those two properties alone decides it.

**This spec requires**: only ever setting the API key property, never `project-id` or `location`, anywhere in `application.properties`, `application-local.properties`, `.env`/`.env.example`, or environment variables. The exact property key for the API key had **conflicting spellings across sources** during research for this spec (`spring.ai.google-genai.api-key` vs `spring.ai.google.genai.api-key`) — the implementer must verify the real property name against the actual `spring-ai-starter-model-google-genai` auto-configuration metadata (e.g., via the IDE's Spring Boot property autocomplete/`additional-spring-configuration-metadata.json`, or the current Spring AI reference docs) rather than guessing, since guessing wrong risks silently landing in the paid Vertex AI path.

Following the existing pattern already used for AWS/JWT secrets in this project:
- `.env.example` gets a new `GOOGLE_GENAI_API_KEY=` line (empty default, matching `AWS_ACCESS_KEY_ID`'s style).
- `application.properties` gets the Spring AI property wired to `${GOOGLE_GENAI_API_KEY:}` (empty default — see Error handling for what happens when it's unset).
- A free key is obtained from Google AI Studio (`https://aistudio.google.com`) — no credit card required, per the design's earlier research; getting the actual key is a manual step for the developer, not something this spec automates.

## Components

Following the project's existing flat package convention (`controller/`, `service/`, `service/impl/`, `dto/` — no feature-based sub-packaging anywhere else in the codebase, so this doesn't introduce one either):

- **`dto/AiPromptRequest.java`**: `{ @NotBlank String prompt }` — mirrors the validation style of every other request DTO in this codebase (e.g. `RegisterRequest`).
- **`dto/AiPromptResponse.java`**: `{ String answer }` — mirrors the `*Response` naming convention (`PaymentResponse`, `VendorResponse`, etc.).
- **`service/AiService.java`** + **`service/impl/AiServiceImpl.java`**: one method, `String ask(String prompt)`, wrapping an injected `ChatClient` (auto-configured by the starter — no manual `ChatClient` bean needed unless the auto-configured one proves insufficient, which isn't expected for this minimal scope).
- **`controller/AiController.java`**: `@RequestMapping("/api/admin/ai")`, class-level `@PreAuthorize("hasRole('ADMIN')")` (matching `AuditLogController`'s exact pattern — admin-only since this hits a real, cost-relevant external API, even if the tier is free). One endpoint: `POST /api/admin/ai/ask` taking `AiPromptRequest`, returning `ApiResponse<AiPromptResponse>` via the existing `ApiResponse.success(message, data)` wrapper.

This is intentionally the smallest possible slice: one request in, one response out, proving the full chain (Spring context boots → Spring AI auto-configures a `ChatClient` → a real network call reaches Gemini → a real answer comes back → it's wrapped in this app's standard response envelope) actually works end to end.

## Error handling

- **No API key configured** (`GOOGLE_GENAI_API_KEY` unset/empty): the application must still start normally — Spring AI's auto-configuration should not fail Spring context startup over a missing key (this matches how the AWS SDK clients in this app already behave: credentials aren't validated until a call is actually made). Only calling `POST /api/admin/ai/ask` should fail, with whatever error Spring AI/the Google client surfaces. This spec does not add a custom mock layer (unlike `aws.mock-enabled`) — that's unnecessary complexity for a smoke-test endpoint whose only job is proving real wiring works; a clear failure when unconfigured is sufficient for v1.
- **Google API call fails** (rate limit, invalid key, network error): the controller lets the exception propagate to this app's existing global exception handler (whatever already handles unhandled exceptions for other controllers) rather than adding bespoke error handling — consistent with how other controllers in this codebase behave, and appropriate since this is a diagnostic endpoint, not a user-facing feature that needs a polished failure mode.

## Testing

- **Version bump verification**: after bumping Spring Boot alone (before adding Spring AI), run the existing build/test cycle and confirm the app still starts and its existing behavior (auth, JPA, payments, etc.) is unaffected. This isolates "did the Boot bump break something" from "did the Spring AI addition break something."
- **One controller test** for `AiController`, using a mocked `ChatClient`/`AiService` so it runs in CI without a real API key or network access — this is the **first test file in `backend/src/test/`** (the project currently has zero tests despite `spring-boot-starter-test` and H2 already being test dependencies), so the implementer is establishing the project's first test setup, not extending an existing one. Keep it minimal: verify a valid request returns 200 with the mocked answer, and that the endpoint is rejected for a non-admin caller (matching the security behavior `@PreAuthorize` is supposed to enforce).
- **Manual end-to-end verification**: with a real free-tier `GOOGLE_GENAI_API_KEY` set locally, start the backend, call `POST /api/admin/ai/ask` as the seeded admin, and confirm a real, non-canned answer comes back from Gemini. This is the actual proof of "Spring AI support," not the mocked unit test above.

## Future work (explicitly out of scope here)

- Using this `ChatClient`/`AiService` foundation to implement the already-approved log-assistant RAG spec's LLM piece, replacing that spec's originally-planned hand-rolled `LlmClient`.
- Swapping or adding providers (e.g., Anthropic Claude, to match what the log-assistant spec originally assumed) via Spring AI's `ChatModel` abstraction.
- Any eventual Spring Boot 4 / Spring AI 2.0 migration, if ever warranted — a separate, much larger project.
- A mock/offline mode for the AI endpoint, if a real need for one emerges (e.g., for CI environments that want to exercise the real HTTP path without hitting Google).
