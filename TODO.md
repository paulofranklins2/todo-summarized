# Todo Insight – TODO

---

# Project Status Overview

## P0 – Core Backend (Complete)

* [x] Initialize Spring Boot project
* [x] Define Todo entity with JPA
* [x] Define enums (TaskStatus, TaskPriority, Role)
* [x] CRUD REST endpoints for todos
* [x] Swagger/OpenAPI documentation
* [x] Validation rules (Jakarta Validation)
* [x] Daily summary endpoint with metrics
* [x] AI summary integration (OpenAI/Gemini)
* [x] Metrics-only fallback if AI fails
* [x] Global exception handling
* [x] Rate limiting for AI endpoints

---

## P1 – Authentication & Security (Complete)

* [x] Spring Security configuration
* [x] User registration and login endpoints
* [x] BCrypt password encoding
* [x] Session-based authentication
* [x] HTTP Basic for API testing
* [x] Role-based authorization (USER, ADMIN)
* [x] User-scoped todo access
* [x] CSRF protection for web, disabled for API

---

## P2 – Frontend UI (Complete)

* [x] Landing page with feature showcase
* [x] Login/Register modals (no separate pages)
* [x] Dashboard with todo list
* [x] Quick-add todo input
* [x] Filters (status, priority)
* [x] Todo detail/edit modal
* [x] Stats modal with metrics breakdown
* [x] AI Insights modal with markdown rendering
* [x] Responsive design (mobile-first)
* [x] Clean, modern UI with CSS variables
* [x] Toast notifications
* [x] Loading states and spinners
* [x] Dashboard footer

---

## P3 – Enhancements (Complete)

* [x] Request/response DTOs
* [x] TodoMapper for entity conversion
* [x] Pagination for todo list
* [x] Filtering by status, priority, due date
* [x] Overdue and upcoming filters
* [x] Audit fields (createdAt, updatedAt)
* [x] Multi-provider AI support (OpenAI, Gemini)
* [x] Graceful AI fallback

---

## P4 – Future Enhancements

* [ ] Add cache to avoid db pulls
* [ ] Tags / categories for todos
* [ ] Cache daily summaries (Redis)
* [ ] Actuator endpoints (health, info, metrics)
* [ ] Dark mode theme
* [ ] Export todos (CSV, JSON)
* [ ] GitHub Actions CI/CD
* [ ] Docker containerization

---

## Testing Coverage

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
* [x] AiProviderSelector tests
* [x] Rate limiting tests

---

