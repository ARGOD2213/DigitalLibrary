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

- **`dto/AiPromptRequest.java`**: `{ @NotBlank @Size(max = 500) String prompt }` — mirrors the validation style of every other request DTO in this codebase (e.g. `RegisterRequest`), and caps length the same way the log-assistant spec's analogous DTO does, since both hit a rate-limited third-party API and admin-only gating alone isn't a reason to skip cheap input validation.
- **`dto/AiPromptResponse.java`**: `{ String answer }` — mirrors the `*Response` naming convention (`PaymentResponse`, `VendorResponse`, etc.).
- **`service/AiService.java`** + **`service/impl/AiServiceImpl.java`**: one method, `String ask(String prompt)`, wrapping an injected Spring AI chat client (auto-configured by the starter — check at implementation time whether the starter auto-configures a ready-to-use `ChatClient` bean directly or a `ChatClient.Builder` that needs one `.build()` call; either is fine, this is a one-line detail not worth pinning down further here).
- **`controller/AiController.java`**: `@RequestMapping("/api/admin/ai")`, class-level `@PreAuthorize("hasRole('ADMIN')")` (matching `AuditLogController`'s exact pattern — admin-only since this hits a real, cost-relevant external API, even if the tier is free). One endpoint: `POST /api/admin/ai/ask` taking `AiPromptRequest`, returning `ApiResponse<AiPromptResponse>` via the existing `ApiResponse.success(message, data)` wrapper.

This is intentionally the smallest possible slice: one request in, one response out, proving the full chain (Spring context boots → Spring AI auto-configures a `ChatClient` → a real network call reaches Gemini → a real answer comes back → it's wrapped in this app's standard response envelope) actually works end to end.

## Error handling

- **No API key configured** (`GOOGLE_GENAI_API_KEY` unset/empty): the *hoped-for* behavior is that the application still starts normally and only `POST /api/admin/ai/ask` fails when actually called. But this is **not guaranteed the way an earlier draft of this spec assumed by analogy to the AWS clients** — this app's AWS clients are hand-written `@Bean` methods (`AwsConfig.java`) with deliberately lazy credential resolution, a choice this project made; the Google GenAI starter's `@AutoConfiguration` is third-party code whose bean-construction-time behavior (eager validation vs. lazy) is not under this project's control. The implementer must check this directly first (start the app locally with the key unset and see what actually happens) before assuming either outcome or designing around it.
  - **If the app starts fine without a key** (most likely — many Spring Boot autoconfigurations already internally guard their own bean definitions with `@ConditionalOnProperty` on their required config, meaning "no key" may already mean "skip creating the bean gracefully" with no project-side workaround needed): nothing further to do here beyond a clear error message when the unconfigured `AiService` is actually called.
  - **If the app fails to start without a key**: this is a real open design question, not a simple bolt-on fix — a `@ConditionalOnProperty` on this project's own `AiServiceImpl` bean would **not** help, since Spring Boot autoconfiguration beans are constructed eagerly during context refresh regardless of what (if anything) ends up depending on them; gating a downstream consumer doesn't prevent an upstream bean's own construction from failing. The two realistic paths at that point are: (a) exclude the starter's autoconfiguration via `spring.autoconfigure.exclude` when no key is present — which requires something more than a static property (a `@Profile`-conditioned properties file, or an `EnvironmentPostProcessor`) since plain `application.properties` can't conditionally exclude based on another property's value — or (b) accept and document "a `GOOGLE_GENAI_API_KEY` is required to run the backend locally at all once this merges" as a hard constraint rather than building extra plumbing to avoid it. Which of these is worth doing is a judgment call for whoever hits this during implementation, informed by how disruptive it actually turns out to be in practice.
- **Google API call fails** (rate limit, invalid key, network error): the controller lets the exception propagate to this app's existing global exception handler (`GlobalExceptionHandler`'s catch-all `@ExceptionHandler(Exception.class)`, confirmed already present) rather than adding bespoke error handling — consistent with how other controllers in this codebase behave, and appropriate since this is a diagnostic endpoint, not a user-facing feature that needs a polished failure mode.
- **No frontend/UI component in this slice** — verification is via a direct HTTP call (curl/Postman/the manual test below) as the seeded admin, not through the React app. A UI for this is out of scope until there's an actual feature behind it.
- The existing global Bucket4j rate limiter (60 req/min, shared across the whole API, per the log-assistant spec's own findings) applies to this new endpoint automatically, same as every other endpoint — worth knowing given this one is explicitly cost-relevant, but no new rate-limiting work is needed here.

## Testing

- **Version bump verification**: after bumping Spring Boot alone (before adding Spring AI), run the existing build/test cycle (`backend/src/test/java/com/digitallibrary/` already has a real integration suite — `BaseIntegrationTest` plus `AuthSecurityIntegrationTest`, `BookCatalogIntegrationTest`, `EngagementIntegrationTest`, `PaymentWebhookSecurityIntegrationTest`) and confirm every existing test still passes. This isolates "did the Boot bump break something" from "did the Spring AI addition break something."
- **One integration test** for `AiController`, following the existing convention exactly: extend `BaseIntegrationTest` (full `@SpringBootTest` + `MockMvc` + H2, per its existing setup), use its `createUser(email, "ROLE_ADMIN")` + `tokenFor(user)` helpers to authenticate as an admin the same way the existing security tests do, and `@MockBean` the new `AiService` so the integration suite never makes a real network call to Google. This is **not** the project's first test file — it's a new test class in an established suite, and should live in a package consistent with the existing `com.digitallibrary.security`/`com.digitallibrary.catalog`/`com.digitallibrary.engagement` test-package style rather than mirroring `src/main`'s flat layout. Keep assertions minimal: a valid admin request returns 200 with the mocked answer; a non-admin caller gets rejected, matching the existing security tests' pattern (e.g. `AuthSecurityIntegrationTest`).
  - **Needs verification at implementation time, not assumed here**: whether the Google GenAI starter's auto-configuration requires a real API key to *construct* its `ChatModel`/`ChatClient` bean at Spring context startup (some auto-configured clients validate eagerly, others — like this app's existing AWS SDK usage — don't until a call is made). If it does, the full `@SpringBootTest` context in `AiControllerTest` will fail to load even with `AiService` mocked away, since Spring still needs to construct the bean `AiServiceImpl` would otherwise depend on. If that turns out to be the case, follow this project's existing precedent for unneeded-in-tests infrastructure: `backend/src/test/resources/application.properties` already excludes Redis's auto-configuration entirely for tests (`spring.autoconfigure.exclude=...RedisAutoConfiguration...`) rather than pointing it at a fake host — do the same for the Google GenAI auto-configuration class if eager validation turns out to be real, rather than committing a placeholder API key value to the test properties file.
- **Manual end-to-end verification**: with a real free-tier `GOOGLE_GENAI_API_KEY` set locally, start the backend, call `POST /api/admin/ai/ask` as the seeded admin, and confirm a real, non-canned answer comes back from Gemini. This is the actual proof of "Spring AI support," not the mocked integration test above.

## Future work (explicitly out of scope here)

- Using this `ChatClient`/`AiService` foundation to implement the already-approved log-assistant RAG spec's LLM piece, replacing that spec's originally-planned hand-rolled `LlmClient`.
- Swapping or adding providers (e.g., Anthropic Claude, to match what the log-assistant spec originally assumed) via Spring AI's `ChatModel` abstraction.
- Any eventual Spring Boot 4 / Spring AI 2.0 migration, if ever warranted — a separate, much larger project.
- A mock/offline mode for the AI endpoint, if a real need for one emerges (e.g., for CI environments that want to exercise the real HTTP path without hitting Google).
