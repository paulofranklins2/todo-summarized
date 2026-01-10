# Todo Insight - Design Notes

A simple design overview of the Todo Insight application architecture and the main choices behind it.

---

## 1. Overview

Todo Insight is a full-stack todo app with a normal todo workflow (CRUD) and an optional AI summary feature. It uses a straightforward Spring Boot layered structure to keep the code easy to navigate and maintain.

---

## 2. Architecture

### High-level view

```mermaid
flowchart TB
  B[Browser<br/>Thymeleaf + JS + CSS] -->|HTTP| A[Spring Boot App]

  subgraph A[Spring Boot App]
    C[Controllers<br/>REST + Web]
    S[Services<br/>Business logic]
    R[Repositories<br/>JPA]
    SEC[Security<br/>JWT]
    AI[AI Integration<br/>OpenAI / Gemini]
    C --> S --> R
    C --> SEC
    S --> AI
  end

  R --> DB[(PostgreSQL)]
```

### Package structure

```
org.duckdns.todosummarized/
├── config/         # Security, cache, API configs
├── controller/     # REST + web controllers
├── domains/
│   ├── entity/     # JPA entities (User, Todo, AiInsight)
│   └── enums/      # Status, Priority, Role
├── dto/            # Request/Response DTOs + mapping
├── exception/      # Global error handling
├── ratelimit/      # Rate limit (AOP)
├── repository/     # Spring Data JPA repositories
└── service/        # Business logic
```

---

## 3. Core design decisions

### 3.1 Layered structure

Keep responsibilities clear:

* Controller: HTTP handling + validation + mapping
* Service: business rules + transactions
* Repository: database access
* Entity: persistence model

### 3.2 Security (JWT)

JWT for API endpoints. Token is validated on each request.

```mermaid
sequenceDiagram
  participant Client
  participant Auth as AuthController
  participant Sec as JwtFilter
  participant Api as Todo/Summary APIs

  Client->>Auth: POST /api/auth/signin
  Auth-->>Client: JWT

  Client->>Api: Request + Authorization: Bearer <jwt>
  Sec->>Sec: Validate token
  Sec-->>Api: Authenticated request
  Api-->>Client: Response
```

### 3.3 AI integration

AI is optional and designed to fail safely (fallback to metrics-only).

```mermaid
flowchart LR
  Entry[AiSummaryService] --> Selector[AiProviderSelector]
  Selector --> OA[OpenAI Adapter]
  Selector --> GM[Gemini Adapter]
  Entry --> Fallback[Metrics-only fallback]
```

Notes:

* Provider selection is centralized (one place to choose).
* If AI is down, return a summary based on stored metrics instead of erroring.
* Cache AI results to reduce cost and latency.

### 3.4 Data model

User-owned todos, plus stored AI insight records.

```mermaid
erDiagram
  USER ||--o{ TODO : owns
  USER ||--o{ AI_INSIGHT : has
```

---

## 4. Workflows

### 4.1 Create todo

```mermaid
sequenceDiagram
  participant UI
  participant C as TodoController
  participant S as TodoService
  participant R as TodoRepository
  participant DB

  UI->>C: POST /api/todos (payload)
  C->>C: validate + map DTO
  C->>S: createTodo()
  S->>R: save(todo)
  R->>DB: INSERT
  DB-->>R: row
  R-->>S: entity
  S-->>C: response DTO
  C-->>UI: 201 Created
```
  

### 4.2 Generate AI summary (with fallback)

```mermaid

sequenceDiagram
  participant UI
  participant C as SummaryController
  participant S as SummaryService
  participant A as AiSummaryService
  participant P as Provider(OpenAI/Gemini)
  participant Cache as Caffeine/DB

  UI->>C: GET /api/summary/ai
  C->>S: buildDailyMetrics()
  S-->>C: metrics

  C->>A: generateNarrative(metrics)
  A->>Cache: check cached insight
  alt cache hit
    Cache-->>A: cached narrative
  else cache miss
    A->>P: request summary
    alt provider ok
      P-->>A: narrative
      A->>Cache: store narrative
    else provider fails
      A-->>C: fallback (no narrative)
    end
  end

  C-->>UI: summary response (metrics + optional narrative)
```

---

## 5. API surface

| Method | Endpoint        | Purpose                                 |
| ------ | --------------- | --------------------------------------- |
| GET    | /api/todos      | List todos (paginated)                  |
| POST   | /api/todos      | Create todo                             |
| PUT    | /api/todos/{id} | Update todo                             |
| DELETE | /api/todos/{id} | Delete todo                             |
| GET    | /api/summary/ai | Metrics summary + optional AI narrative |

Error handling stays consistent via a global exception handler:

* 400 validation issues
* 401/403 auth issues
* 404 not found
* 500 unexpected errors

---

## 6. Cross-cutting concerns

* Rate limiting: simple AOP guard on expensive endpoints (like summary generation)
* Caching: cache AI summaries, still store the deterministic metrics
* Validation: Bean Validation on request DTOs
* Logging: log important failures (AI provider errors, auth failures), avoid logging secrets/tokens

---

## 7. Tech choices

* Spring Boot: strong defaults, easy testing, common patterns
* PostgreSQL: reliable relational storage
* Thymeleaf + Vanilla JS: simple delivery, no frontend build pipeline
* JWT: stateless API auth
* Caffeine: fast local caching

---

## Summary

The design stays simple:

* standard Spring layering
* JWT auth for API
* AI is optional and isolated behind a small interface
* fallback behavior keeps the app usable even when AI fails
* caching reduces cost and response time
