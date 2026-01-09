# Scaffolding TODO list

## P0

* [x] Initialize Spring Boot project (Java 17, Maven)
* [x] README setup steps (this file) kept accurate
* [x] Create a TODO.md file
* [x] Add dependencies: Web, Validation, JPA, Thymeleaf, Security, PostgreSQL
* [ ] Docker Compose for Postgres (db + volume)
* [x] Define Todo entity + enums (TaskStatus, TaskPriority)
* [ ] Flyway migration: `todos` table with indexes (dueDate, status, priority)
* [x] CRUD endpoints for todos (REST Controller)
* [x] Validation rules (title required, max lengths, valid priority)
* [x] Daily summary endpoint returning deterministic metrics
* [ ] AI summary service adapter (feature-flagged, timeout, retries)
* [ ] Fallback behavior (metrics-only summary if AI disabled/fails)
* [x] Basic error handling (global exception handler, clean error responses)
* [x] Unit tests for TodoMapper (17 tests, 100% coverage)
* [x] Unit tests for GlobalExceptionHandler (13 tests, 80%+ coverage)
* [x] Unit tests for TodoService (20 tests, 80%+ coverage)
* [x] Unit tests for UserService (24 tests, 80%+ coverage)
* [x] Unit tests for SecurityConfig (8 tests, 80%+ coverage)
* [x] Unit tests for TodoController (8 tests, 80%+ coverage)
* [x] Unit tests for SummaryService (10 tests, 80%+ coverage)
* [x] Unit tests for SummaryController (3 tests, 80%+ coverage)
* [ ] Integration tests (Testcontainers + repository)
* [x] Swagger/OpenAPI documentation


## P1

* [x] Request/response DTOs (TodoRequestDTO, TodoResponseDTO, TodoMapper)
* [x] Pagination for `GET /todos` (via Spring Data Pageable)
* [x] Add filtering: status, priority, dueDate range, overdue, upcoming (TodoQuery + TodoSpecs)
* [x] Add audit fields (createdAt/updatedAt) and automatic timestamps (@PrePersist/@PreUpdate)
* [ ] Add structured logging (request id / correlation id)
* [ ] Actuator configuration (health/info/metrics)
* [ ] GitHub Actions CI: build + test on PR
* [ ] Add AI prompt versioning (`promptVersion` field in response)
* [ ] Add rate limiting or simple abuse protection for summary endpoint

## P2

* [ ] Add simple Thymeleaf frontend for user experience
* [x] Add Spring Security for user login/logout and account management
* [ ] Add user accounts + JWT auth
* [ ] Add tags/categories
* [ ] Weekly summary endpoint
* [ ] Cache daily summaries for a short TTL
* [ ] Add a minimal frontend (optional) or CLI client
* [ ] Deploy to a free host (Render/Fly.io) with env-based config
