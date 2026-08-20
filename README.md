# Digital Library

A full-stack digital publishing and library platform — readers browse and buy books,
vendors publish and sell their own catalog, and admins moderate the whole thing.
Started as a learning project; grew into a real multi-role platform.

## What it does

- **Readers**: browse/search the catalog, subscribe to plans, buy books, leave reviews,
  favorite titles, track reading history, get recommendations.
- **Vendors**: apply for a vendor account, publish books (single files or ZIP bundles via S3),
  track commissions on sales.
- **Admins**: approve vendor applications, review audit logs, moderate the catalog.
- **Platform**: JWT auth with refresh-token rotation, role-based access (reader/vendor/admin),
  Redis-backed caching and rate limiting, AOP-based audit logging, payment webhooks
  (mock/Stripe/Razorpay), AWS S3/SES/SNS integration (mockable for local dev).

## Tech stack

| | |
|---|---|
| Backend | Java 17, Spring Boot, Spring Security, Spring Data JPA, Flyway |
| Frontend | React 18, MUI |
| Data | PostgreSQL, Redis |
| Infra | Docker, GitHub Actions CI/CD |
| Cloud | AWS (S3, SES, SNS) — mocked locally, real in deployed environments |

## Project structure

```text
backend/
  src/main/java/com/digitallibrary/
    config/        CORS, security, and web config
    controller/    REST API endpoints
    dto/           Request/response objects
    entity/        JPA database models
    exception/     Global error handling
    repository/    Spring Data JPA interfaces
    service/       Business logic interfaces
    service/impl/  Business logic implementations
    security/      JWT, auth filters
    ratelimit/     Bucket4j rate limiting
    audit/         AOP audit logging
  src/test/java/com/digitallibrary/  integration test suite (H2, MockMvc)

frontend/
  src/
    components/    React UI components
    services/      API call functions

tools/
  traffic-bot/     local-only dev tool for exercising the app under realistic traffic
```

## Run locally

Postgres and Redis run in Docker; the backend and frontend run natively for hot reload.
From the repo root:

```powershell
docker compose up -d postgres redis
```

Backend (from `backend/`):

```powershell
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Backend URL: `http://localhost:8000` — check `http://localhost:8000/api/health`.

Frontend (from `frontend/`):

```powershell
npm install
npm start
```

Frontend URL: `http://localhost:3000`.

On first run, `DataInitializer` auto-seeds demo accounts and sample books into an empty
database — safe to run repeatedly, it skips anything that already exists:

| Role | Email | Password |
|---|---|---|
| Admin | admin@library.com | admin123 |
| Reader | user@library.com | user123 |
| Vendor | vendor@library.com | vendor123 |

(Local-only seed data — not real credentials for any live environment.)

## Delivery workflow

Changes go through pull requests to `main`; GitHub Actions runs the backend and frontend
test/build pipeline before merge.
