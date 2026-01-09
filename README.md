# AI Daily Summary Todo API

A backend-first Todo app that keeps the scope small while still showing real engineering fundamentals. It stores daily todos, exposes a clean REST API, and generates an end-of-day summary using deterministic metrics plus an optional AI-generated narrative.

---

## Tech Stack

- **Java 17** with **Spring Boot 4.0**
- **Spring Data JPA** with PostgreSQL
- **Spring Security** (configured for future auth)
- **Thymeleaf** (for future frontend)
- **Bean Validation** (Jakarta Validation)
- **Lombok** for boilerplate reduction
- **JUnit 5 + Mockito** for testing

---

## Project Structure

```
src/main/java/org/duckdns/todosummarized/
├── config/              # Configuration classes (SecurityConfig, TimeConfig)
├── controller/          # REST controllers (TODO)
├── domains/
│   ├── entity/          # JPA entities (Todo, User)
│   └── enums/           # TaskStatus, TaskPriority
├── dto/                 # Data Transfer Objects
│   ├── TodoMapper.java          # Entity <-> DTO mapping
│   ├── TodoRequestDTO.java      # Todo input validation
│   ├── TodoResponseDTO.java     # Todo API response
│   ├── UserRegistrationDTO.java # User registration input
│   └── UserResponseDTO.java     # User API response
├── exception/           # Global exception handling
│   ├── GlobalExceptionHandler.java
│   ├── ErrorResponse.java
│   ├── TodoNotFoundException.java
│   ├── InvalidTodoException.java
│   ├── DuplicateTodoException.java
│   └── UserAlreadyExistsException.java
├── repository/          # Data access layer
│   ├── TodoRepository.java
│   ├── UserRepository.java
│   ├── TodoQuery.java       # Search criteria record
│   └── spec/
│       └── TodoSpecs.java   # JPA Specifications for filtering
└── service/
    ├── TodoService.java     # Todo business logic
    └── UserService.java     # User registration & profile
```

---

## TODO

Track development progress in [TODO.md](TODO.md)
