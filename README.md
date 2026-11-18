# QueueLess — Smart Physical Queue & Crowd Management Platform

QueueLess solves a real-world problem: people waste hours physically standing in queues at hospitals, colleges, government offices, banks, and customer service centers.

The platform allows users to digitally join a queue, monitor their live position and dynamic estimated wait time, leave the physical venue, and receive automated real-time notifications when their turn approaches.

---

## Tech Stack

### Frontend
- **Framework**: React 19, TypeScript, Vite
- **Styling**: Tailwind CSS
- **Routing**: React Router DOM v6
- **Data Fetching**: Axios
- **Charts**: Recharts
- **Icons**: Lucide React
- **Real-Time**: STOMP WebSocket Client (`@stomp/stompjs`)

### Backend
- **Java**: Java 21
- **Framework**: Spring Boot 3.2.4
- **Security**: Spring Security + JWT (Access + Refresh tokens)
- **Database**: JPA / Hibernate (MySQL / H2 in-memory profile)
- **Caching**: Redis
- **Real-Time**: Spring WebSocket (STOMP Broker)
- **Documentation**: SpringDoc OpenAPI / Swagger UI
- **Build Tool**: Maven

---

## Folder Structure

```text
queueless/
├── backend/
│   ├── src/main/java/com/queueless/
│   │   ├── config/          # Security, WebSocket, Redis, DataSeeder
│   │   ├── controller/      # REST API Controllers
│   │   ├── dto/             # Request & Response DTOs
│   │   ├── entity/          # JPA Entities
│   │   ├── repository/      # Spring Data Repositories
│   │   ├── security/        # JWT Authentication & UserDetailsService
│   │   ├── service/         # Queue Engine, Estimation, Prediction & Auth Services
│   │   └── scheduler/       # Scheduled cleanup tasks
│   ├── Dockerfile
│   └── pom.xml
├── frontend/
│   ├── src/
│   │   ├── api/             # Axios instance & interceptors
│   │   ├── components/      # Layout & Header
│   │   ├── context/         # AuthContext & ThemeContext
│   │   ├── hooks/           # useWebSocket custom hook
│   │   ├── pages/           # Explore, UserDashboard, StaffDashboard, AdminDashboard, Login, Register
│   │   └── types/           # TypeScript interfaces
│   ├── Dockerfile
│   └── package.json
├── docker-compose.yml
├── .env.example
└── README.md
```

---

## Test Credentials (Pre-seeded Demo Accounts)

| Role | Email | Password |
|---|---|---|
| **User (Customer / Student)** | `user@queueless.com` | `password123` |
| **Staff (Counter Agent)** | `staff@queueless.com` | `password123` |
| **Admin (Org Admin)** | `admin@queueless.com` | `password123` |

---

## Local Running Instructions

### Backend
```bash
cd backend
mvn spring-boot:run
```
Swagger UI will be available at: `http://localhost:8080/swagger-ui.html`

### Frontend
```bash
cd frontend
npm install
npm run dev
```
Frontend application will run at: `http://localhost:5173`

---

## Running with Docker Compose

```bash
docker-compose up --build
```
This launches MySQL, Redis, Spring Boot Backend, and Nginx Frontend concurrently.

---

## Architectural Highlights
- **Concurrency Locking**: Utilizes transactional pessimistic row locks (`findByIdForUpdate`) and synchronized sequence generation to ensure zero duplicate tokens under simultaneous heavy traffic.
- **Dynamic Waiting Engine**: Calculates wait times based on historical service duration, active counter count, and queue sequence.
- **Leave & Return Thresholds**: Users configure custom notification thresholds (15, 10, or 5 people ahead) and receive live alerts via WebSocket without refreshing.
- **Statistical Queue Prediction Engine**: Forecasts hourly crowd density and provides automated counter scaling recommendations for administrators.
