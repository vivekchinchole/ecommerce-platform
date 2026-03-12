# 🛒 E-Commerce Microservices Platform

A production-grade microservices system built with Spring Boot 3, Kafka, Redis, and Docker.

---

## 🏗️ Architecture

```
Client
  ↓
API Gateway (Port: 8080) — JWT Auth + Rate Limiting (Redis)
  ↓
Eureka Server (Port: 8761) — Service Discovery
  ↓
┌──────────────┬──────────────┬──────────────┬──────────────┐
│ User Service │Product Svc   │ Order Svc    │Notification  │
│  Port: 8081  │  Port: 8082  │  Port: 8083  │  Port: 8084  │
│  PostgreSQL  │  PostgreSQL  │  PostgreSQL  │   MongoDB    │
│              │  Redis Cache │  Kafka Pub   │  Kafka Sub   │
└──────────────┴──────────────┴──────────────┴──────────────┘
```

---

## 🚀 Quick Start

### Prerequisites
- Docker & Docker Compose
- Java 17+
- Maven 3.8+

### 1. Build all services
```bash
# Build each service
cd eureka-server && mvn clean package -DskipTests && cd ..
cd api-gateway && mvn clean package -DskipTests && cd ..
cd user-service && mvn clean package -DskipTests && cd ..
cd product-service && mvn clean package -DskipTests && cd ..
cd order-service && mvn clean package -DskipTests && cd ..
cd notification-service && mvn clean package -DskipTests && cd ..
```

### 2. Start all services with Docker
```bash
docker-compose up -d
```

### 3. Verify services are running
- Eureka Dashboard: http://localhost:8761
- API Gateway: http://localhost:8080
- User Service Swagger: http://localhost:8081/swagger-ui.html
- Product Service Swagger: http://localhost:8082/swagger-ui.html
- Order Service Swagger: http://localhost:8083/swagger-ui.html

---

## 📋 API Usage

### Register & Login
```bash
# Register
POST http://localhost:8080/auth/register
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "password123",
  "role": "CUSTOMER"
}

# Login
POST http://localhost:8080/auth/login
{
  "email": "john@example.com",
  "password": "password123"
}
# Returns JWT token → use in Authorization: Bearer <token>
```

### Products
```bash
# Create product (ADMIN)
POST http://localhost:8080/products
Authorization: Bearer <token>
{
  "name": "iPhone 15",
  "description": "Latest iPhone",
  "price": 999.99,
  "stockQuantity": 100,
  "category": "Electronics"
}

# List products (paginated)
GET http://localhost:8080/products?page=0&size=10
```

### Orders
```bash
# Place order
POST http://localhost:8080/orders
Authorization: Bearer <token>
{
  "userId": 1,
  "items": [
    { "productId": 1, "quantity": 2, "price": 999.99 }
  ]
}

# Get user orders
GET http://localhost:8080/orders/user/1
Authorization: Bearer <token>

# Cancel order (PENDING only)
DELETE http://localhost:8080/orders/1
Authorization: Bearer <token>
```

---

## 🔧 Tech Stack

| Component | Technology |
|-----------|-----------|
| Framework | Spring Boot 3.2 |
| Service Discovery | Netflix Eureka |
| API Gateway | Spring Cloud Gateway |
| Security | Spring Security + JWT |
| Messaging | Apache Kafka |
| Caching | Redis |
| ORM | Spring Data JPA |
| NoSQL | MongoDB (Notifications) |
| Circuit Breaker | Resilience4j |
| Documentation | OpenAPI / Swagger |
| Testing | JUnit 5 + Mockito |
| Containerization | Docker + Docker Compose |

---

## 🧪 Running Tests
```bash
cd user-service && mvn test
cd order-service && mvn test
cd product-service && mvn test
```

---

## 📁 Project Structure
```
ecommerce-platform/
├── docker-compose.yml
├── README.md
├── eureka-server/
├── api-gateway/
├── user-service/
├── product-service/
├── order-service/
└── notification-service/
```

---

## 🌟 Features Implemented
- ✅ JWT Authentication & Authorization
- ✅ Role-based access (ADMIN / CUSTOMER)
- ✅ Redis Caching with TTL + Cache Invalidation
- ✅ Redis Rate Limiting (100 req/min per IP)
- ✅ Kafka Event Streaming (order.placed / order.cancelled)
- ✅ Optimistic Locking on Order entity
- ✅ Circuit Breaker with Resilience4j
- ✅ Feign Client for inter-service communication
- ✅ Global Exception Handling
- ✅ Pagination on list endpoints
- ✅ OpenAPI / Swagger documentation
- ✅ Unit Tests (JUnit 5 + Mockito)
- ✅ Docker + Docker Compose setup
