# Spring AI Integration Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bump this project's backend to Spring Boot 3.5.16, wire up Spring AI 1.1.8 with Google's free-tier Gemini Developer API, and prove the whole chain works end to end via one admin-only smoke-test endpoint.

**Architecture:** A version bump (isolated and regression-tested on its own first), then a thin vertical slice — `dto/AiPromptRequest` → `AiController` → `AiService`/`AiServiceImpl` → Spring AI's auto-configured `ChatClient` → Google Gemini — following this codebase's existing controller/service/service-impl/dto conventions exactly, with one integration test in the existing `BaseIntegrationTest`-based suite.

**Tech Stack:** Spring Boot 3.5.16, Spring AI 1.1.8 (`spring-ai-starter-model-google-genai`), Google Gemini Developer API (free tier, `gemini-2.5-flash`).

**Spec:** `docs/superpowers/specs/2026-08-20-spring-ai-integration-design.md`

---

## Chunk 1: Spring AI Integration

### Facts resolved during planning (the spec deliberately left these open — now answered against the current, real docs)

- API key property: **`spring.ai.google.genai.api-key`** (dot-separated `google.genai`, not hyphenated `google-genai` — one of two spellings the spec flagged as conflicting; this is the confirmed correct one per the current Spring AI reference docs).
- Model property: **`spring.ai.google.genai.chat.options.model`**. Use **`gemini-2.5-flash`** — Google's guidance as of March 2026 is that new projects should use `gemini-2.5-flash`/`gemini-2.5-flash-lite` rather than the older `gemini-2.0-flash-001` default, which is being phased out for new projects.
- Official toggle to disable the chat auto-configuration entirely: **`spring.ai.model.chat=none`** (default is `google-genai`, i.e. enabled). This is a real, documented Spring AI property — use this in test config instead of guessing an internal auto-configuration class name for `spring.autoconfigure.exclude`, if Task 2's investigation finds it's needed.
- Starter artifact: `org.springframework.ai:spring-ai-starter-model-google-genai`. BOM: `org.springframework.ai:spring-ai-bom:1.1.8`.
- Spring Boot `3.5.16` is the **final** 3.5.x release — Spring Boot 3.5 reached open-source end-of-life on 2026-06-30, so this version will not receive further OSS security patches. This doesn't change the spec's recommendation (the alternative was Spring Boot 4.0, a much larger migration this spec explicitly avoids), but it's worth knowing: this project will be on a line that's done receiving patches, not just "behind." If that matters for how this is prioritized against other work, that's a call for whoever's driving the project, not something this plan resolves.
- **Still genuinely unresolved, and Task 2 below investigates it directly rather than guessing further**: whether the `google-genai` starter's auto-configuration fails Spring context startup when `GOOGLE_GENAI_API_KEY` is blank. Web research did not settle this either way.

### File Structure

```
backend/pom.xml                                                    modify: version bump, new BOM entry, new dependency
.env.example                                                        modify: new GOOGLE_GENAI_API_KEY= line
backend/src/main/resources/application.properties                   modify: new Google GenAI config block
backend/src/main/resources/application-local.properties             modify: same, for local-profile runs
backend/src/test/resources/application.properties                   modify: only if Task 2 finds startup fails without a key
backend/src/main/java/com/digitallibrary/dto/AiPromptRequest.java   create
backend/src/main/java/com/digitallibrary/dto/AiPromptResponse.java  create
backend/src/main/java/com/digitallibrary/service/AiService.java     create
backend/src/main/java/com/digitallibrary/service/impl/AiServiceImpl.java  create
backend/src/main/java/com/digitallibrary/controller/AiController.java     create
backend/src/test/java/com/digitallibrary/ai/AiControllerTest.java   create
```

---

### Task 1: Bump Spring Boot to 3.5.16 and verify nothing broke

**Files:**
- Modify: `backend/pom.xml:13`

- [ ] **Step 1: Change the parent version**

In `backend/pom.xml`, change:
```xml
    <version>3.2.5</version>
```
to:
```xml
    <version>3.5.16</version>
```
(This is the `<parent><version>` under `spring-boot-starter-parent`, around line 13 — not any other version string in the file.)

- [ ] **Step 2: Rebuild and run the full existing test suite**

Run (from `backend/`): `mvn clean test`

Expected: `BUILD SUCCESS`, and all existing tests pass — at minimum `AuthSecurityIntegrationTest`, `BookCatalogIntegrationTest`, `EngagementIntegrationTest`, `PaymentWebhookSecurityIntegrationTest` (16 tests total per this suite's last known state). If anything fails here, it's the version bump surfacing a breaking change (e.g. in Spring Security's auto-configuration defaults) — stop and fix that before proceeding to Task 2, so a later failure can't be confused with something Spring AI caused.

- [ ] **Step 3: Start the app locally and confirm it still runs**

Run (from `backend/`): `mvn spring-boot:run -Dspring-boot.run.profiles=local`
Then: `curl -s http://localhost:8000/api/health`
Expected: `{"success":true,...,"data":{"backend":"UP","database":"UP","redis":"UP"},...}` (same as before the bump — requires the local Postgres/Redis containers from the main repo README already running). Stop the app (Ctrl+C) once confirmed.

- [ ] **Step 4: Commit**

```bash
git add backend/pom.xml
git commit -m "Bump Spring Boot 3.2.5 -> 3.5.16 to unlock Spring AI 1.1.x"
```

---

### Task 2: Add Spring AI dependency and config, investigate the unresolved startup question

**Files:**
- Modify: `backend/pom.xml`
- Modify: `.env.example`
- Modify: `backend/src/main/resources/application.properties`
- Modify: `backend/src/main/resources/application-local.properties`

- [ ] **Step 1: Add the Spring AI BOM to `backend/pom.xml`'s existing `<dependencyManagement>` block**

Find the existing block (it currently has only the AWS SDK BOM):
```xml
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>bom</artifactId>
        <version>2.25.10</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>
```
Add the Spring AI BOM as a second entry in the same `<dependencies>` list:
```xml
      <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-bom</artifactId>
        <version>1.1.8</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
```
(Check Maven Central for anything newer than `1.1.8` in the `1.1.x` line before finalizing — this was current as of planning time but Spring AI ships patches frequently.)

- [ ] **Step 2: Add the starter dependency to `backend/pom.xml`'s `<dependencies>` block**

Add near the other starters (e.g. after the AWS SDK entries):
```xml
    <!-- Spring AI — Google Gemini (free-tier Developer API) -->
    <dependency>
      <groupId>org.springframework.ai</groupId>
      <artifactId>spring-ai-starter-model-google-genai</artifactId>
    </dependency>
```
(No `<version>` needed — it comes from the BOM added in Step 1.)

- [ ] **Step 3: Add config to `.env.example`**

Add near the existing `AWS_ACCESS_KEY_ID=` line:
```
# Google GenAI (Spring AI) — free-tier Gemini Developer API key from https://aistudio.google.com
# Leave blank locally if you don't need the AI endpoint yet.
GOOGLE_GENAI_API_KEY=
```

- [ ] **Step 4: Add config to `backend/src/main/resources/application.properties`**

Add a new block, e.g. after the AWS Configuration section:
```properties
# Google GenAI (Spring AI) — free-tier Gemini Developer API.
# IMPORTANT: never add spring.ai.google.genai.project-id or
# spring.ai.google.genai.location here or anywhere else — setting either
# one silently switches this from the free API-key mode to paid Vertex AI
# mode, which will reject this key with a 400 auth error.
spring.ai.google.genai.api-key=${GOOGLE_GENAI_API_KEY:}
spring.ai.google.genai.chat.options.model=gemini-2.5-flash
```

- [ ] **Step 5: Mirror the same block in `backend/src/main/resources/application-local.properties`**

This file already exists (local-profile overrides for the containerized local Postgres/Redis setup). Add the same two lines from Step 4 to it, so local-profile runs pick up the key the same way.

- [ ] **Step 6: Investigate — does the app start with `GOOGLE_GENAI_API_KEY` unset?**

Make sure `GOOGLE_GENAI_API_KEY` is *not* set in your shell environment or `.env` (only the blank default in `application.properties` should apply), then run (from `backend/`):
```
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

**If it starts successfully** (reaches "Started DigitalLibraryApplication..." and `/api/health` responds): good — no further action needed for this task. Note this outcome in the commit message for this task.

**If it fails to start** (an exception during context refresh, likely naming `GoogleGenAiChatAutoConfiguration` or similar): this confirms the spec's flagged risk is real. Do **not** try `@ConditionalOnProperty` on `AiServiceImpl` — that was already established as ineffective (it wouldn't run early enough to stop the upstream auto-configured bean's own construction). Instead, pick one of the two paths the spec named, and note which one and why in the commit message:
  - **(a)** Exclude the chat auto-configuration by default in `application-local.properties` (`spring.ai.model.chat=none`), and require developers to override it (e.g. via an environment variable or a personal properties override) when they actually have a key and want the feature on. Downside: the feature is off by default for everyone, including developers who do have a key, unless they take an extra step.
  - **(b)** Accept and document in the main repo README that `GOOGLE_GENAI_API_KEY` is now required to run the backend locally at all, full stop. Simpler, but raises the bar for anyone setting up the project fresh.

  Given this project's stated priority (staying easy to run locally for someone picking it up fresh), **(a) is likely the better default** if this branch is hit — but confirm by actually seeing the failure and its exact cause before deciding, since the fix should address the real error, not a guessed one.

- [ ] **Step 7: Commit**

```bash
git add backend/pom.xml .env.example backend/src/main/resources/application.properties backend/src/main/resources/application-local.properties
git commit -m "Add Spring AI dependency and Google Gemini free-tier config"
```
(If Step 6 required a config change beyond what's listed above, include that file in this commit too, and describe the finding in the commit message.)

---

### Task 3: Request/response DTOs

**Files:**
- Create: `backend/src/main/java/com/digitallibrary/dto/AiPromptRequest.java`
- Create: `backend/src/main/java/com/digitallibrary/dto/AiPromptResponse.java`

- [ ] **Step 1: Write `AiPromptRequest.java`**

```java
package com.digitallibrary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AiPromptRequest {

    @NotBlank(message = "Prompt is required")
    @Size(max = 500, message = "Prompt must be at most 500 characters")
    private String prompt;

    public AiPromptRequest() {}

    public AiPromptRequest(String prompt) {
        this.prompt = prompt;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
}
```

- [ ] **Step 2: Write `AiPromptResponse.java`**

```java
package com.digitallibrary.dto;

public class AiPromptResponse {

    private String answer;

    public AiPromptResponse() {}

    public AiPromptResponse(String answer) {
        this.answer = answer;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}
```

- [ ] **Step 3: Verify it compiles**

Run (from `backend/`): `mvn compile`
Expected: `BUILD SUCCESS`

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/digitallibrary/dto/AiPromptRequest.java backend/src/main/java/com/digitallibrary/dto/AiPromptResponse.java
git commit -m "Add AI prompt request/response DTOs"
```

---

### Task 4: `AiService` and `AiServiceImpl`

**Files:**
- Create: `backend/src/main/java/com/digitallibrary/service/AiService.java`
- Create: `backend/src/main/java/com/digitallibrary/service/impl/AiServiceImpl.java`

- [ ] **Step 1: Write the interface**

```java
package com.digitallibrary.service;

public interface AiService {
    String ask(String prompt);
}
```

- [ ] **Step 2: Write the implementation**

```java
package com.digitallibrary.service.impl;

import com.digitallibrary.service.AiService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class AiServiceImpl implements AiService {

    private final ChatClient chatClient;

    public AiServiceImpl(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public String ask(String prompt) {
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }
}
```

**Note:** this constructor takes `ChatClient.Builder` (the auto-configured bean the `spring-ai-starter-model-google-genai` starter provides) and builds a `ChatClient` once at startup. If compilation fails because the starter instead auto-configures a ready-built `ChatClient` bean directly rather than a `Builder`, change the constructor to accept `ChatClient chatClient` directly and drop the `.build()` call — this is the one place the spec flagged as an implementation-time detail rather than a fixed fact.

- [ ] **Step 3: Verify it compiles**

Run (from `backend/`): `mvn compile`
Expected: `BUILD SUCCESS`. If it fails on the `ChatClient.Builder` constructor parameter, apply the fallback described in Step 2's note and recompile.

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/digitallibrary/service/AiService.java backend/src/main/java/com/digitallibrary/service/impl/AiServiceImpl.java
git commit -m "Add AiService wrapping the Spring AI ChatClient"
```

---

### Task 5: `AiController`

**Files:**
- Create: `backend/src/main/java/com/digitallibrary/controller/AiController.java`

- [ ] **Step 1: Write the controller**

```java
package com.digitallibrary.controller;

import com.digitallibrary.dto.ApiResponse;
import com.digitallibrary.dto.AiPromptRequest;
import com.digitallibrary.dto.AiPromptResponse;
import com.digitallibrary.service.AiService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/ai")
@PreAuthorize("hasRole('ADMIN')")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/ask")
    public ResponseEntity<ApiResponse<AiPromptResponse>> ask(@Valid @RequestBody AiPromptRequest request) {
        String answer = aiService.ask(request.getPrompt());
        return ResponseEntity.ok(ApiResponse.success("AI response generated", new AiPromptResponse(answer)));
    }
}
```

(This matches `AuditLogController`'s exact pattern: class-level `@RequestMapping("/api/admin/...")` + class-level `@PreAuthorize("hasRole('ADMIN')")`, constructor injection, `ApiResponse.success(message, data)`.)

- [ ] **Step 2: Verify it compiles**

Run (from `backend/`): `mvn compile`
Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/digitallibrary/controller/AiController.java
git commit -m "Add admin-only POST /api/admin/ai/ask endpoint"
```

---

### Task 6: Integration test

**Files:**
- Create: `backend/src/test/java/com/digitallibrary/ai/AiControllerTest.java`
- Modify (only if needed — see Step 2): `backend/src/test/resources/application.properties`

- [ ] **Step 1: Write the test, following `AuthSecurityIntegrationTest`'s exact structure**

```java
package com.digitallibrary.ai;

import com.digitallibrary.BaseIntegrationTest;
import com.digitallibrary.dto.AiPromptRequest;
import com.digitallibrary.entity.AppUser;
import com.digitallibrary.service.AiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class AiControllerTest extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AiService aiService;

    @Test
    void ask_WithAdminToken_ShouldReturn200AndAnswer() throws Exception {
        when(aiService.ask(anyString())).thenReturn("mocked answer");

        AppUser admin = createUser("aiadmin@example.com", "ROLE_ADMIN");
        String adminToken = tokenFor(admin);

        AiPromptRequest request = new AiPromptRequest("What is Spring AI?");

        mockMvc.perform(post("/api/admin/ai/ask")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.answer").value("mocked answer"));
    }

    @Test
    void ask_WithUserToken_ShouldReturn403Forbidden() throws Exception {
        AppUser user = createUser("aiuser@example.com", "ROLE_USER");
        String userToken = tokenFor(user);

        AiPromptRequest request = new AiPromptRequest("What is Spring AI?");

        mockMvc.perform(post("/api/admin/ai/ask")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void ask_WithoutToken_ShouldReturn401Unauthorized() throws Exception {
        AiPromptRequest request = new AiPromptRequest("What is Spring AI?");

        mockMvc.perform(post("/api/admin/ai/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
```

`@MockitoBean` (non-deprecated replacement for `@MockBean`) replaces the real `AiService` bean in the test's Spring context, so no real network call to Google ever happens in this suite — matching the plan's requirement that CI runs without needing a real API key.

- [ ] **Step 2: Run the new tests**

Run (from `backend/`): `mvn test -Dtest=AiControllerTest`

**Expected: `Tests run: 3, Failures: 0, Errors: 0`.**

**If instead the whole Spring context fails to load** (an error mentioning `GoogleGenAiChatAutoConfiguration` or similar, not a test assertion failure): this means the auto-configured `ChatModel`/`ChatClient` bean itself fails to construct without a real API key, even though `AiService` — the bean built *from* it — is mocked away. `@MockitoBean` only replaces `AiService`; it doesn't stop Spring from still trying to construct the `ChatClient` bean `AiServiceImpl` would have depended on, because auto-configured beans are built during context refresh regardless of what ends up using them.

  Fix: add to `backend/src/test/resources/application.properties`, following the same reasoning already used there to exclude Redis's auto-configuration for tests:
  ```properties
  # No real Google GenAI calls in tests — AiService is @MockitoBean'd in AiControllerTest,
  # but the underlying ChatClient bean would otherwise still try to construct without
  # a real API key and fail context startup.
  spring.ai.model.chat=none
  ```
  Then re-run Step 2's command and confirm it passes.

- [ ] **Step 3: Run the full test suite to confirm nothing else broke**

Run (from `backend/`): `mvn test`
Expected: `BUILD SUCCESS`, all tests passing (the pre-existing 16 plus this task's 3 new ones — 19 total, or the count from whatever Task 1 found as the baseline).

- [ ] **Step 4: Commit**

```bash
git add backend/src/test/java/com/digitallibrary/ai/AiControllerTest.java
# if Step 2's fallback was needed:
git add backend/src/test/resources/application.properties
git commit -m "Add AiController integration test"
```

---

### Task 7: Manual end-to-end verification with a real key

**Files:** none (verification only)

- [ ] **Step 1: Get a free Gemini Developer API key**

Visit `https://aistudio.google.com`, sign in, and create an API key (no credit card required, per the design spec's research).

- [ ] **Step 2: Set the key locally**

Add to your local `.env` (not `.env.example` — that stays blank):
```
GOOGLE_GENAI_API_KEY=<your real key here>
```

- [ ] **Step 3: Start the backend with the key set**

Run (from `backend/`): `mvn spring-boot:run -Dspring-boot.run.profiles=local`
Expected: starts normally, `curl -s http://localhost:8000/api/health` returns `"backend":"UP"`.

- [ ] **Step 4: Log in as the seeded admin and call the new endpoint**

```bash
curl -s -X POST http://localhost:8000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@library.com","password":"admin123"}'
```
Copy the `accessToken` from the response, then:
```bash
curl -s -X POST http://localhost:8000/api/admin/ai/ask \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <accessToken from above>" \
  -d '{"prompt":"In one sentence, what is Spring AI?"}'
```

**Expected:** `{"success":true,"message":"AI response generated","data":{"answer":"<a real, non-canned sentence about Spring AI from Gemini>"},...}`. A real, coherent answer here — not a mocked string, not an error — is the actual proof that Spring AI support works end to end. If this fails, the error message is the next debugging lead (auth error → check the API key and that `project-id`/`location` were never set anywhere per the spec's warning; any other error → consult the current Spring AI Google GenAI reference docs for what that specific error means).
