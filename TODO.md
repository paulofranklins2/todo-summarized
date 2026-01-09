# Scaffolding TODO list

---

# Todo Summarized – TODO

**Last updated:** 2026-01-09

---

## P0 – Core (Critical)

* [x] Initialize Spring Boot project
* [x] Keep README setup instructions accurate
* [x] Create TODO.md
* [x] Add dependencies
* [x] Define Todo entity
* [x] Define enums
* [x] CRUD REST endpoints for todos
* [x] Swagger/OpenAPI documentation for all endpoints
* [x] Validation rules
* [x] Daily summary endpoint
* [x] AI summary adapter
* [x] Metrics-only fallback if AI fails or is disabled
* [x] Global exception handling with structured error responses
* [x] Swagger security schemes configured

---

## P1 – Enhancements

* [x] Request/response DTOs
* [x] TodoMapper
* [x] Pagination for `GET /todos`
* [x] Filtering
    * [x] Status
    * [x] Priority
    * [x] Due date range
    * [x] Overdue
    * [x] Upcoming
* [x] Audit fields (createdAt, updatedAt)
* [x] Rate limiting for summary endpoint

---

## P2 – Future

* [ ] Simple Thymeleaf UI
* [x] Spring Security configuration
* [x] User registration and login endpoints
* [x] User roles
* [x] JWT authentication
* [ ] Tags / categories
* [ ] Weekly summary endpoint
* [ ] Cache daily summaries
* [ ] Actuator (health, info, metrics)
* [ ] Deploy to hosting
* [ ] GitHub Actions CI/CD

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
