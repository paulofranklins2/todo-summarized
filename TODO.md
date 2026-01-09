# Scaffolding TODO list

## P0

* [x] Initialize Spring Boot project (Java 17, Maven)
* [x] README setup steps (this file) kept accurate
* [x] Create a TODO.md file
* [ ] Add dependencies: Web, Validation, JPA, Flyway, Actuator, OpenAPI
* [ ] Docker Compose for Postgres (db + volume)
* [ ] Define Todo entity + enums (Status, Priority P0/P1/P2)
* [ ] Flyway migration: `todos` table with indexes (dueDate, status, priority)
* [ ] CRUD endpoints for todos
* [ ] Validation rules (title required, max lengths, valid priority)
* [ ] Daily summary endpoint returning deterministic metrics
* [ ] AI summary service adapter (feature-flagged, timeout, retries)
* [ ] Fallback behavior (metrics-only summary if AI disabled/fails)
* [ ] Basic error handling (global exception handler, clean error responses)
* [ ] Unit tests for summary calculation logic
* [ ] Integration tests (Testcontainers + repository)
* [ ] Swagger/OpenAPI documentation


## P1

* [ ] Request/response DTOs (no entities exposed directly)
* [ ] Pagination for `GET /todos`
* [ ] Add filtering: status, priority
* [ ] Add audit fields (createdAt/updatedAt) and automatic timestamps
* [ ] Add structured logging (request id / correlation id)
* [ ] Actuator configuration (health/info/metrics)
* [ ] GitHub Actions CI: build + test on PR
* [ ] Add AI prompt versioning (`promptVersion` field in response)
* [ ] Add rate limiting or simple abuse protection for summary endpoint

## P2

* [ ] Add simple Thymeleaf frontend for user experience
* [ ] Add Spring Security for user login/logout and account management
* [ ] Add user accounts + JWT auth
* [ ] Add tags/categories
* [ ] Weekly summary endpoint
* [ ] Cache daily summaries for a short TTL
* [ ] Add a minimal frontend (optional) or CLI client
* [ ] Deploy to a free host (Render/Fly.io) with env-based config
