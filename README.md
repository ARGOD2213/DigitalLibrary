# Digital Library - Full Stack Learning Project

Beginner-friendly full-stack app using:

- Backend: Java Spring Boot
- Database: PostgreSQL local
- Frontend: React.js

This version is still local-only. No Docker files, no AWS deployment files, and no cloud database yet.

## Current Features

- Add a book
- View books with pagination
- Search books by title or author
- Sort books by title, author, category, or available copies
- Edit a book
- Delete a book
- Consistent backend API response format
- Validation and beginner-friendly error handling
- Health endpoint for future load balancer checks

## Project Structure

```text
backend/
  src/main/java/com/digitallibrary/
    config/        CORS and web config
    controller/    REST API endpoints
    dto/           Request/response objects
    entity/        JPA database models
    exception/     Global error handling
    repository/    Spring Data JPA interfaces
    service/       Business logic interfaces
    service/impl/  Business logic implementations

frontend/
  src/
    components/    React UI components
    services/      API call functions
```

## PostgreSQL Setup

Create the database once:

```sql
CREATE DATABASE digital_library;
```

Current backend database config lives in:

```text
backend/src/main/resources/application.properties
```

Current local values:

```properties
server.port=8000
spring.datasource.url=jdbc:postgresql://localhost:5432/digital_library
spring.datasource.username=postgres
spring.datasource.password=postgres
```

## Run Locally

Backend:

```powershell
cd "C:\Users\NAVEEN ROYAL\Digital Library\backend"
mvn spring-boot:run
```

Backend URL:

```text
http://localhost:8000
```

Frontend:

```powershell
cd "C:\Users\NAVEEN ROYAL\Digital Library\frontend"
npm install
npm start
```

Frontend URL:

```text
http://localhost:3000
```

## API Endpoints

```text
GET    /api/health
POST   /api/books
GET    /api/books?page=0&size=6&sortBy=title&sortDirection=asc
GET    /api/books/{id}
GET    /api/books/search?keyword=java&page=0&size=6
PUT    /api/books/{id}
DELETE /api/books/{id}
```

Example POST body:

```json
{
  "title": "Java Basics",
  "author": "Beginner Author",
  "category": "Programming",
  "isbn": "ISBN-LOCAL-001",
  "availableCopies": 3
}
```

## Future Learning Roadmap

- Security: add Spring Security, login, JWT, roles such as ADMIN and USER
- Pagination: connect page size selector and server-side table filters
- Redis: cache common searches or all-books pages
- Rate limiting: protect APIs using Bucket4j or a reverse proxy rule
- Reverse proxy: put Nginx in front of React and Spring Boot
- Load balancing: run multiple backend instances behind a load balancer
- Docker: containerize backend after local setup is understood
- AWS ECR: push backend Docker image to AWS ECR
- AWS RDS: move PostgreSQL from local PC to managed RDS
- S3 and CloudFront: host React build as static frontend
- ECS or EKS: deploy backend container in AWS
