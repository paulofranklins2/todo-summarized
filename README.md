# AI Daily Summary Todo API

A backend-first Todo app that keeps the scope small while still showing real engineering fundamentals. It stores daily
todos, exposes a clean REST API, and generates an end-of-day summary using deterministic metrics plus an optional
AI-generated narrative.

---

## Tech Stack

- **Java 17** with **Spring Boot 4.0**
- **Spring Data JPA** with PostgreSQL
- **Spring Security** (configured for future auth)
- **Thymeleaf** (for future frontend)
- **Bean Validation** (Jakarta Validation)
- **Lombok** for boilerplate reduction
- **JUnit 5 + Mockito** for testing
- **Springdoc OpenAPI** for Swagger UI and API documentation

---

## Project Structure

```
src/main/java/org/duckdns/todosummarized/
├── config/                 # Security, time, app config
├── controller/             # REST controllers
│   ├── TodoController.java
│   └── SummaryController.java
├── domain/
│   ├── entity/             # JPA entities
│   │   ├── Todo.java
│   │   └── User.java
│   └── enums/              # Domain enums
│       ├── TaskStatus.java
│       └── TaskPriority.java
├── dto/
│   ├── todo/
│   │   ├── TodoRequestDTO.java
│   │   ├── TodoResponseDTO.java
│   │   └── TodoMapper.java
│   ├── user/
│   │   ├── UserRegistrationDTO.java
│   │   └── UserResponseDTO.java
│   └── DailySummaryDTO.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── ErrorResponse.java
│   ├── TodoNotFoundException.java
│   ├── InvalidTodoException.java
│   ├── DuplicateTodoException.java
│   └── UserAlreadyExistsException.java
├── repository/
│   ├── TodoRepository.java
│   ├── UserRepository.java
│   └── spec/
│       └── TodoSpecs.java
├── service/
│   ├── TodoService.java
│   ├── UserService.java
│   └── SummaryService.java
└── search/
    └── TodoQuery.java
```

---

## API Documentation

This project uses **Springdoc OpenAPI** for interactive API documentation.

Once the application is running, access:
- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **Swagger UI**: [https://todo-summarized.duckdns.org/swagger-ui.html](https://todo-summarized.duckdns.org/swagger-ui.html)
---

## TODO

Track development progress in [TODO.md](TODO.md)
