# AI Daily Summary Todo API

A backend-first Todo app that keeps the scope small while still showing real engineering fundamentals. It stores daily
todos, exposes a clean REST API, and generates an end-of-day summary using deterministic metrics plus an optional
AI-generated narrative.

---

## Project Status

| Category | Progress |
|----------|----------|
| Core Functionality (P0) | 85% Complete |
| Enhanced Features (P1) | 60% Complete |
| Future Enhancements (P2) | 30% Complete |

**Key Milestones Completed:**
- Full CRUD REST API with validation and error handling
- User authentication (registration, login, session-based + HTTP Basic)
- Daily summary with deterministic metrics
- Comprehensive unit test coverage (100+ tests)
- Swagger/OpenAPI documentation

See [TODO.md](TODO.md) for detailed progress tracking.

---

## Tech Stack

- **Java 21** with **Spring Boot 4.0.1**
- **Spring Data JPA** with PostgreSQL
- **Spring Security** (session-based auth + HTTP Basic for API)
- **Thymeleaf** (dependency added for future frontend)
- **Bean Validation** (Jakarta Validation)
- **Lombok** for boilerplate reduction
- **JUnit 5 + Mockito** for testing
- **Springdoc OpenAPI** for Swagger UI and API documentation

---

## Project Structure

```
src/main/java/org/duckdns/todosummarized/
├── config/                 # Security, OpenAPI, time config
│   ├── SecurityConfig.java
│   ├── OpenApiConfig.java
│   ├── OpenAiProperties.java
│   └── TimeConfig.java
├── controller/             # REST controllers
│   ├── AuthController.java
│   ├── TodoController.java
│   └── SummaryController.java
├── domains/
│   ├── entity/             # JPA entities
│   │   ├── Todo.java
│   │   └── User.java
│   └── enums/              # Domain enums
│       ├── TaskStatus.java
│       ├── TaskPriority.java
│       └── Role.java
├── dto/
│   ├── TodoRequestDTO.java
│   ├── TodoResponseDTO.java
│   ├── TodoMapper.java
│   ├── UserRegistrationDTO.java
│   ├── UserLoginDTO.java
│   ├── UserResponseDTO.java
│   └── DailySummaryDTO.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── ErrorResponse.java
│   ├── TodoNotFoundException.java
│   ├── InvalidTodoException.java
│   ├── DuplicateTodoException.java
│   ├── UnauthorizedAccessException.java
│   └── UserAlreadyExistsException.java
├── repository/
│   ├── TodoRepository.java
│   ├── UserRepository.java
│   ├── TodoQuery.java
│   ├── spec/
│   │   └── TodoSpecs.java
│   └── projection/
│       ├── StatusCountProjection.java
│       └── PriorityCountProjection.java
└── service/
    ├── TodoService.java
    ├── UserService.java
    ├── SummaryService.java
    └── CustomUserDetailsService.java
```

---

## API Documentation

This project uses **Springdoc OpenAPI** for interactive API documentation.

Once the application is running, access:
- **Swagger UI (Local)**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **Swagger UI (Production)**: [https://todo-summarized.duckdns.org/swagger-ui.html](https://todo-summarized.duckdns.org/swagger-ui.html)
- **OpenAPI Spec**: `/v3/api-docs`

---

## TODO

Track development progress in [TODO.md](TODO.md)
