# Scaffolding TODO list

---

# Todo Summarized – TODO

**Last updated:** 2026-01-09

---

## P0 – Core (Critical)

* [x] Initialize Spring Boot project (Java 17, Spring Boot 4.0.1)
* [x] Keep README setup instructions accurate
* [x] Create TODO.md
* [x] Add dependencies (Web, Validation, JPA, Security, PostgreSQL, Thymeleaf)
* [x] Define Todo entity
* [x] Define enums (TaskStatus, TaskPriority)
* [ ] Flyway migration for `todos` table
    * [ ] Index: due_date
    * [ ] Index: status
    * [ ] Index: priority
* [x] CRUD REST endpoints for todos
* [x] Swagger/OpenAPI documentation for all endpoints
* [x] Validation rules (title required, max lengths, valid enums)
* [x] Daily summary endpoint (metrics only)
* [x] AI summary adapter
* [x] Metrics-only fallback if AI fails or is disabled
* [x] Global exception handling with structured error responses
* [ ] Integration tests
* [x] Swagger security schemes configured

---

## P1 – Enhancements

* [x] Request/response DTOs
* [x] TodoMapper
* [x] Pagination for `GET /todos` (max 20 per page)
* [x] Filtering
    * [x] Status
    * [x] Priority
    * [x] Due date range
    * [x] Overdue
    * [x] Upcoming
* [x] Audit fields (createdAt, updatedAt)
* [ ] Structured logging (request id / correlation id)
* [ ] Actuator (health, info, metrics)
* [ ] GitHub Actions CI (build + test)
* [ ] AI prompt versioning in response
* [x] Rate limiting for summary endpoint

---

## P2 – Future

* [ ] Simple Thymeleaf UI
* [x] Spring Security configuration
* [x] User registration and login endpoints
* [x] User roles
* [ ] JWT authentication
* [ ] Tags / categories
* [ ] Weekly summary endpoint
* [ ] Cache daily summaries
* [ ] CLI client or minimal frontend
* [ ] Deploy to hosting

---

## Testing

* [x] TodoMapper unit tests
* [x] GlobalExceptionHandler tests
* [x] TodoService tests
* [x] UserService tests
* [x] SecurityConfig tests
* [x] TodoController tests
* [x] SummaryService tests
* [x] SummaryController tests
* [x] AuthController tests
* [x] CustomUserDetailsService tests
* [ ] Integration tests

---

## Implemented (Quick Reference)

### Authentication

* [x] User signup and signing
* [x] BCrypt password encoding
* [x] Form login
* [x] HTTP Basic (API testing)
* [x] Role-based authorization
* [x] CSRF disabled for API

### Todos

* [x] User-scoped CRUD
* [x] Status update (PATCH)
* [x] Search and filtering
* [x] Pagination

### Daily Summary

* [x] Status counts
* [x] Priority counts
* [x] Overdue count
* [x] Upcoming count
* [x] Due today count
* [x] Completion rate
