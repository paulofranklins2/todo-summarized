# Todo Insight

A modern, full-stack Todo application with AI-powered insights. Built with Spring Boot and a clean, responsive UI.

**Live Demo:** [https://todo-insight.duckdns.org](https://todo-insight.duckdns.org) | **Design Doc:** [docs/DESIGN.md](docs/DESIGN.md)

---

## Features

### Core Functionality
- **Todo Management** – Create, read, update, delete todos with title, description, status, priority, and due date
- **Smart Filtering** – Filter by status (Not Started, In Progress, Completed, Cancelled) and priority (Low, Medium, High, Critical)
- **Pagination** – Efficiently browse large todo lists

### AI-Powered Insights
- **Daily Summary** – Get AI-generated insights about your productivity
- **Multi-Provider Support** – Works with OpenAI GPT and Google Gemini
- **Graceful Fallback** – Shows metrics-only summary if AI is unavailable

### User Experience
- **Modern Dashboard** – Clean, compact todo list with quick-add functionality
- **Responsive Design** – Works seamlessly on desktop, tablet, and mobile
- **Real-time Feedback** – Toast notifications, loading states, and smooth animations
- **Stats Overview** – View completion rates and breakdowns by status/priority

### Security
- **JWT Authentication** – Stateless API authentication
- **User Isolation** – Each user only sees their own todos
- **Rate Limiting** – Protection against API abuse

---

## 🛠 Tech Stack

| Layer | Technology |
|-------|------------|
| **Backend** | Java 21, Spring Boot 4.0.1 |
| **Database** | PostgreSQL with Spring Data JPA |
| **Security** | Spring Security + JWT |
| **Frontend** | Thymeleaf, Vanilla JS (ES6+), Custom CSS |
| **AI** | OpenAI GPT / Google Gemini |
| **Caching** | Caffeine (in-memory) |
| **API Docs** | Springdoc OpenAPI / Swagger UI |
| **Testing** | JUnit 5, Mockito (100+ tests) |

---

## 📸 Screenshots

<details>
<summary><strong>🖥️ Desktop Screenshots</strong></summary>

### Landing Page
Clean, modern landing page showcasing features with login/register modals.

![Landing Page 1](docs/images/landing-page-1.png)
![Landing Page 2](docs/images/landing-page-2.png)
![Landing Page 3](docs/images/landing-page-3.png)

### Dashboard
Compact todo list with integrated filters, quick-add, and action buttons.

![Dashboard 1](docs/images/dashboard-1.png)
![Dashboard 2](docs/images/dashboard-2.png)
![Dashboard 3](docs/images/dashboard-3.png)
![Dashboard 4](docs/images/dashboard-4.png)
![Dashboard 5](docs/images/dashboard-5.png)
![Dashboard 6](docs/images/dashboard-6.png)

### AI Insights
AI-generated productivity insights with markdown formatting.

![AI Insights 1](docs/images/ai-insights-1.png)
![AI Insights 2](docs/images/ai-insights-2.png)

</details>

<details>
<summary><strong>📱 Mobile Screenshots</strong></summary>

Responsive design for seamless mobile experience.

![Mobile 1](docs/images/mobile-1.png)
![Mobile 2](docs/images/mobile-2.png)
![Mobile 3](docs/images/mobile-3.png)
![Mobile 4](docs/images/mobile-4.png)
![Mobile 5](docs/images/mobile-5.png)
![Mobile 6](docs/images/mobile-6.png)
![Mobile 7](docs/images/mobile-7.png)
![Mobile 8](docs/images/mobile-8.png)

</details>

---

## 📚 API Documentation

Interactive API documentation available at:
- **Local:** http://localhost:8080/swagger-ui.html
- **Production:** https://todo-insight.duckdns.org/swagger-ui.html

---

## Development Progress

See [TODO.md](TODO.md) for detailed progress tracking.

---

## License

This project is for educational and portfolio purposes.

