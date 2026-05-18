# ⚡ Order Matching Engine

A production-grade, real-time order matching engine built with Spring Boot — the core component of a trading exchange. Implements price-time priority matching, event-driven architecture, and a live WebSocket market data feed.

[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.0-brightgreen)](https://spring.io/projects/spring-boot)
[![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-3.7-black)](https://kafka.apache.org/)
[![Redis](https://img.shields.io/badge/Redis-7-red)](https://redis.io/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-ready-blue)](https://www.docker.com/)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-manifests-326CE5)](https://kubernetes.io/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## 📋 Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Key Features](#key-features)
- [System Design](#system-design)
- [Getting Started](#getting-started)
- [API Documentation](#api-documentation)
- [WebSocket Market Data](#websocket-market-data)
- [Configuration](#configuration)
- [Running Tests](#running-tests)
- [Docker Deployment](#docker-deployment)
- [Kubernetes Deployment](#kubernetes-deployment)
- [Observability](#observability)
- [Project Structure](#project-structure)
- [Design Decisions](#design-decisions)
- [Interview Talking Points](#interview-talking-points)

---

## 🔍 Overview

This project implements the **core matching engine** of a trading exchange — the same architectural patterns used by Zerodha, Binance, and similar platforms. It handles concurrent order submissions, maintains a real-time order book, matches orders using price-time priority, and disseminates market data to WebSocket clients.

### What makes this production-grade?

| Concern | Solution |
|---|---|
| Concurrency | LMAX Disruptor lock-free ring buffer — no thread contention |
| Ordering | Single-threaded engine per instrument — deterministic matching |
| Persistence | PostgreSQL with optimistic locking — no double-spending |
| Reliability | Kafka for trade events — at-least-once delivery |
| Caching | Caffeine L1 + Redis L2 — 95%+ cache hit rate |
| Resilience | Resilience4j circuit breakers + retry + fallback |
| Security | JWT + RBAC + Redis token-bucket rate limiting |
| Observability | Prometheus + Grafana + Zipkin distributed tracing |

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        CLIENT LAYER                         │
│          REST API Client          WebSocket Client          │
└──────────────────┬─────────────────────────┬───────────────┘
                   │                         │
┌──────────────────▼─────────────────────────▼───────────────┐
│                         API LAYER                           │
│   JWT Filter → Order Controller → Order Validator → ...     │
└──────────────────┬──────────────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────────────┐
│                    MATCHING ENGINE LAYER                     │
│                                                             │
│   HTTP Thread                                               │
│       │                                                     │
│       ▼                                                     │
│   Disruptor Ring Buffer (lock-free)                         │
│       │                                                     │
│       ▼                                                     │
│   Matching Engine Thread (single per instrument)            │
│       │                                                     │
│       ├──── TreeMap<Price, Deque<Order>> BID side           │
│       └──── TreeMap<Price, Deque<Order>> ASK side           │
│                   │                                         │
│                   ▼                                         │
│           TradeEvent emitted                                │
└───────────────────┬─────────────────────────────────────────┘
                    │
┌───────────────────▼─────────────────────────────────────────┐
│                      ASYNC PIPELINE                          │
│                                                             │
│   Kafka Producer ──► topic: trade.executed                  │
│                                  │                          │
│   Kafka Consumer ◄────────────────┘                         │
│       │                                                     │
│       ├── Persist Trade + TradeLeg to PostgreSQL            │
│       ├── Settle Balances (deductReserved + credit)         │
│       ├── Update Order Status (FILLED/PARTIALLY_FILLED)     │
│       ├── Publish to Redis pub/sub channels                 │
│       └── WebSocket push to subscribers                     │
└─────────────────────────────────────────────────────────────┘
```

### Request Flow

```
POST /api/v1/orders
    │
    ├─► JWT Authentication (filter)
    ├─► Rate Limit Check (Redis token bucket, Lua script)
    ├─► Idempotency Check (clientOrderId lookup)
    ├─► Instrument Validation (Caffeine L1 → Redis L2 → DB)
    ├─► Balance Reservation (PostgreSQL pessimistic lock)
    ├─► Persist Order (status: PENDING → OPEN)
    ├─► Publish to Disruptor Ring Buffer
    │       └─► 202 Accepted returned to client immediately
    │
    └─► [Async] Matching Engine Thread
            ├─► Price-time priority matching
            ├─► TradeEvent emitted
            └─► [Async] Kafka → Consumer → DB + WebSocket
```

---

## 🛠️ Tech Stack

### Core
| Technology | Version | Purpose |
|---|---|---|
| Java | 21 | Language (Virtual Threads ready) |
| Spring Boot | 3.3.0 | Application framework |
| Spring Security | 6.x | JWT auth + RBAC |
| Spring Data JPA | 3.x | ORM layer |
| Spring Kafka | 3.x | Event streaming |
| Spring WebSocket | 3.x | Real-time market data |
| LMAX Disruptor | 4.0.0 | Lock-free inter-thread messaging |

### Infrastructure
| Technology | Version | Purpose |
|---|---|---|
| PostgreSQL | 16 | Primary datastore |
| Apache Kafka | 3.7 | Trade event streaming |
| Redis | 7 | Caching + rate limiting + pub/sub |
| Flyway | 10.x | Database migrations |

### Resilience & Observability
| Technology | Purpose |
|---|---|
| Resilience4j | Circuit breakers + retry |
| Micrometer | Metrics collection |
| Prometheus | Metrics storage |
| Grafana | Metrics visualization |
| Zipkin | Distributed tracing |
| Caffeine | In-JVM L1 cache |

### DevOps
| Technology | Purpose |
|---|---|
| Docker | Containerization (multi-stage build) |
| Kubernetes | Orchestration + HPA |
| GitHub Actions | CI/CD pipeline |

---

## ✨ Key Features

### 🔄 Order Matching Engine
- **Price-time priority** — best price first, FIFO within same price level
- **Order types** — LIMIT, MARKET
- **Partial fills** — remaining quantity re-enters the order book
- **Multi-instrument** — independent engine per symbol (BTC-USD, ETH-USD, SOL-USD)
- **Crash recovery** — reloads open orders from DB on restart

### 📊 Order Book
- **In-memory TreeMap** — O(log n) insertion and lookup
- **Bid side** — descending by price (highest bid first)
- **Ask side** — ascending by price (lowest ask first)
- **Depth snapshots** — top 10 levels per side for WebSocket clients

### ⚡ Performance
- **LMAX Disruptor** — lock-free ring buffer (1024 slots), single engine thread
- **No locks** — matching engine processes one order at a time per instrument
- **Async everything** — trade persistence, balance settlement, WebSocket push are all off the critical path
- **Cache warmup** — instrument cache pre-loaded on startup

### 🔐 Security
- **JWT authentication** — HS256, 24-hour expiry, claims: userId + role
- **RBAC** — ROLE_TRADER, ROLE_MARKET_MAKER, ROLE_ADMIN
- **Rate limiting** — Redis token bucket via Lua script (10 orders/sec burst 20)
- **Idempotency** — clientOrderId prevents duplicate order submission
- **Optimistic locking** — account version prevents race conditions on balance

### 📡 Real-Time Market Data
- **STOMP over WebSocket** — SockJS fallback for browser compatibility
- **Public channels** — `/topic/{symbol}/orderbook` and `/topic/{symbol}/trades`
- **Private channel** — `/user/queue/orders` for personal fill notifications
- **Redis pub/sub** — enables multi-node WebSocket fan-out

---

## 🎯 System Design

### Order Book Data Structure

```java
// Bid side — buyers, highest price first
TreeMap<BigDecimal, Deque<Order>> bids =
    new TreeMap<>(Comparator.reverseOrder());

// Ask side — sellers, lowest price first
TreeMap<BigDecimal, Deque<Order>> asks =
    new TreeMap<>(Comparator.naturalOrder());
```

The `Deque<Order>` at each price level maintains FIFO ordering — first order placed at a price level gets filled first (time priority).

### Matching Algorithm

```
1. Incoming BUY order with price P
2. Look at best ask (lowest ask price)
3. If best ask ≤ P → MATCH
   a. Fill qty = min(incoming.qty, resting.qty)
   b. Fill price = resting order price (aggressor gets resting price)
   c. Reduce both quantities
   d. Emit TradeEvent
   e. If resting order fully filled → remove from book
   f. Repeat until incoming filled or no more crossable orders
4. If incoming has remaining qty → add to bid side of book
```

### Balance Reservation Model

```
Account has two balance fields:
  available_balance  ← can place new orders
  reserved_balance   ← locked by open orders

Order Placed:    available -= amount,  reserved += amount
Order Filled:    reserved  -= amount,  (asset account credited)
Order Cancelled: reserved  -= amount,  available += amount

CHECK (available_balance >= 0) — DB-level guarantee, last line of defense
```

### Why Single-Threaded Matching?

The matching engine is intentionally single-threaded per instrument:

- **No locks needed** — only one thread touches the order book
- **Deterministic** — same sequence of events always produces same result
- **Auditable** — every trade can be replayed from Kafka event log
- **Scalable** — add instruments, not threads. BTC-USD on node 1, ETH-USD on node 2

---

## 🚀 Getting Started

### Prerequisites

```
Java 21+
Docker Desktop
Maven 3.8+
```

### 1. Clone the Repository

```bash
git clone https://github.com/YOUR_USERNAME/order-matching-engine.git
cd order-matching-engine
```

### 2. Start Infrastructure

```bash
# From project root
docker-compose up -d

# Verify all containers are healthy
docker-compose ps
```

Expected containers:
```
ome_postgres      running (healthy)
ome_redis         running (healthy)
ome_zookeeper     running
ome_kafka         running
ome_zipkin        running
ome_prometheus    running
ome_grafana       running
ome_pgadmin       running
```

### 3. Run the Application

```bash
cd ome
mvn spring-boot:run
```

Application starts at `http://localhost:8080`

### 4. Verify Health

```bash
curl http://localhost:8080/actuator/health
```

Expected:
```json
{
  "status": "UP",
  "components": {
    "db":             { "status": "UP" },
    "redis":          { "status": "UP" },
    "kafka":          { "status": "UP" },
    "matchingEngine": { "status": "UP", "details": { "enginesRegistered": 3 }}
  }
}
```

### 5. Open Live Dashboard

```
http://localhost:8080/dashboard.html
```

---

## 📖 API Documentation

### Authentication

All endpoints except `/api/v1/auth/**` require a Bearer token.

```bash
# Register
POST /api/v1/auth/register
{
  "username": "trader1",
  "email": "trader1@test.com",
  "password": "password123"
}

# Login
POST /api/v1/auth/login
{
  "email": "trader1@test.com",
  "password": "password123"
}
# Response includes accessToken — use as Bearer token
```

### Orders

```bash
# Place Order
POST /api/v1/orders
Authorization: Bearer <token>
{
  "symbol":        "BTC-USD",
  "side":          "BUY",
  "type":          "LIMIT",
  "quantity":      0.5,
  "price":         65000.00,
  "clientOrderId": "unique-id-123"   # optional, for idempotency
}
# Response: 202 Accepted

# Get All Orders (paginated)
GET /api/v1/orders?page=0&size=20
Authorization: Bearer <token>

# Get Single Order
GET /api/v1/orders/{orderId}
Authorization: Bearer <token>

# Cancel Order
DELETE /api/v1/orders/{orderId}
Authorization: Bearer <token>
```

### Market Data (Public)

```bash
# Order Book Snapshot
GET /api/v1/market/orderbook/BTC-USD

# Recent Trades
GET /api/v1/market/trades/BTC-USD

# All Instruments
GET /api/v1/market/instruments
```

### Standard Response Envelope

```json
{
  "success": true,
  "message": "Order accepted for matching",
  "data": { ... },
  "timestamp": "2026-05-16T10:30:00Z"
}
```

```json
{
  "success": false,
  "error": {
    "code":    "INSUFFICIENT_BALANCE",
    "message": "Account has insufficient available balance",
    "fields":  null
  },
  "timestamp": "2026-05-16T10:30:00Z"
}
```

### Error Codes

| Code | HTTP | Description |
|---|---|---|
| `VALIDATION_FAILED` | 400 | Request validation failed |
| `INSUFFICIENT_BALANCE` | 422 | Not enough available balance |
| `INSTRUMENT_DISABLED` | 422 | Trading disabled for symbol |
| `ORDER_NOT_CANCELLABLE` | 409 | Order already filled/cancelled |
| `RATE_LIMIT_EXCEEDED` | 429 | Too many requests |
| `NOT_FOUND` | 404 | Resource not found |
| `SERVICE_UNAVAILABLE` | 503 | Circuit breaker open |

---

## 📡 WebSocket Market Data

### Connect

```javascript
const socket = new SockJS('http://localhost:8080/ws');
const client = Stomp.over(socket);

client.connect(
  { Authorization: 'Bearer ' + token },
  () => {
    // Subscribe to order book updates
    client.subscribe('/topic/BTC-USD/orderbook', msg => {
      const data = JSON.parse(msg.body);
      // { symbol, bids, asks, timestamp, sequence }
    });

    // Subscribe to trade feed
    client.subscribe('/topic/BTC-USD/trades', msg => {
      const data = JSON.parse(msg.body);
      // { tradeId, symbol, price, quantity, side, executedAt }
    });

    // Subscribe to private order fills
    client.subscribe('/user/queue/orders', msg => {
      const data = JSON.parse(msg.body);
      // { orderId, symbol, status, filledQty, remainingQty, lastFillPrice }
    });
  }
);
```

### Live Dashboard

Open `http://localhost:8080/dashboard.html` for a real-time trading dashboard showing live order book depth, trade feed, and personal fill notifications.

---

## ⚙️ Configuration

Key configuration in `src/main/resources/application.yml`:

```yaml
app:
  jwt:
    secret: ${JWT_SECRET}          # 256-bit hex string
    expiration-ms: 86400000        # 24 hours

  matching-engine:
    instruments:
      - BTC-USD
      - ETH-USD
      - SOL-USD

  kafka:
    topics:
      order-events:    order.events
      trade-executed:  trade.executed
      order-book-update: orderbook.update
```

### Environment Variables

| Variable | Description | Default |
|---|---|---|
| `DB_URL` | PostgreSQL connection URL | `jdbc:postgresql://localhost:5432/ome_db` |
| `DB_USERNAME` | Database username | `ome_user` |
| `DB_PASSWORD` | Database password | — |
| `REDIS_HOST` | Redis hostname | `localhost` |
| `REDIS_PASSWORD` | Redis password | — |
| `KAFKA_SERVERS` | Kafka bootstrap servers | `localhost:9092` |
| `JWT_SECRET` | JWT signing secret (256-bit hex) | — |

---

## 🧪 Running Tests

```bash
# Run all tests
mvn test

# Run matching engine unit tests only
mvn test -Dtest=MatchingEngineTest

# Run with coverage report
mvn verify
```

### Matching Engine Test Coverage

```
✅ noMatchOnEmptyBook          — order added to book, no trade
✅ exactMatch                  — full fill both sides
✅ partialFill                 — incoming smaller than resting
✅ pricePriority               — best ask filled first
✅ timePriority                — FIFO at same price level
✅ noMatchWhenPricesDoNotCross — spread maintained
✅ sweepMultipleLevels         — one order fills multiple price levels
```

---

## 🐳 Docker Deployment

### Build Image

```bash
cd ome
docker build -t ome:latest .
```

Multi-stage build:
- **Stage 1 (builder)**: `eclipse-temurin:21-jdk-alpine` — compiles source
- **Stage 2 (runtime)**: `eclipse-temurin:21-jre-alpine` — runs app
- Final image size: ~455MB
- Runs as non-root user `ome`

### Run Container

```bash
docker run -d \
  --name ome-app \
  --network order_maching_engine_default \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e DB_PASSWORD=ome_pass \
  -e REDIS_PASSWORD=ome_redis_pass \
  -e JWT_SECRET=your-secret-here \
  ome:latest
```

---

## ☸️ Kubernetes Deployment

```bash
# Apply all manifests
kubectl apply -f k8s/namespace.yml
kubectl apply -f k8s/configmap.yml
kubectl apply -f k8s/secret.yml
kubectl apply -f k8s/deployment.yml
kubectl apply -f k8s/service.yml
kubectl apply -f k8s/hpa.yml

# Verify deployment
kubectl rollout status deployment/ome-app -n ome

# View pods
kubectl get pods -n ome
```

### Kubernetes Features
- **2 replicas** — high availability
- **Rolling updates** — zero downtime deploys (`maxUnavailable: 0`)
- **HPA** — auto-scales 2→10 pods on CPU > 70% or memory > 80%
- **Liveness probe** — `/actuator/health/liveness` — restarts unhealthy pods
- **Readiness probe** — `/actuator/health/readiness` — removes from LB if not ready
- **Resource limits** — 512Mi/250m requests, 1Gi/1000m limits

---

## 📊 Observability

### Prometheus Metrics

```
http://localhost:9090
```

Custom metrics exposed:

| Metric | Type | Description |
|---|---|---|
| `ome_orders_placed_total` | Counter | Total orders placed |
| `ome_trades_executed_total` | Counter | Total trades executed |
| `ome_order_placement_latency_seconds` | Timer | Order placement P50/P95/P99 |
| `ome_orders_active` | Gauge | Currently active orders |
| `ome_orderbook_bids` | Gauge | Open bid levels |
| `ome_orderbook_asks` | Gauge | Open ask levels |
| `ome_rate_limit_hits_total` | Counter | Rate limit rejections |

### Grafana Dashboard

```
http://localhost:3000
admin / admin
```

Import dashboard ID `12900` for Spring Boot metrics, then add custom OME panels.

### Distributed Tracing (Zipkin)

```
http://localhost:9411
```

Every HTTP request generates a trace showing:
- JWT filter processing time
- Service layer execution
- Database query time
- Kafka publish latency

### Structured Logging

Every log line includes:
```
2026-05-16 10:30:00 [http-nio-8080-exec-1] INFO [traceId,spanId] [requestId] 
  c.p.ome.api.service.OrderService - Order placed: abc123 BUY BTC-USD @ 65000
```

---

## 📁 Project Structure

```
order-matching-engine/
├── ome/
│   ├── src/main/java/com/project/ome/
│   │   ├── api/
│   │   │   ├── controller/        # REST controllers
│   │   │   ├── dto/               # Request/Response DTOs
│   │   │   ├── service/           # Business logic
│   │   │   └── validator/         # Custom validators
│   │   ├── engine/
│   │   │   ├── core/              # MatchingEngine, Registry, Initializer
│   │   │   ├── disruptor/         # LMAX Disruptor configuration
│   │   │   └── model/             # EngineOrder, TradeEvent, OrderBook
│   │   ├── marketdata/
│   │   │   ├── config/            # WebSocket + Redis pub/sub config
│   │   │   └── dto/               # WebSocket message DTOs
│   │   ├── publisher/
│   │   │   ├── TradeEventPublisher.java   # Kafka producer
│   │   │   └── TradeEventConsumer.java    # Kafka consumer
│   │   └── shared/
│   │       ├── cache/             # Instrument + Account cache services
│   │       ├── config/            # Redis, Resilience4j config
│   │       ├── dto/               # ApiResponse, PageResponse
│   │       ├── entity/            # JPA entities
│   │       ├── exception/         # Global exception handler
│   │       ├── observability/     # Metrics, health indicators
│   │       ├── ratelimit/         # Token bucket rate limiter
│   │       ├── repository/        # Spring Data repositories
│   │       └── security/          # JWT filter, SecurityConfig
│   ├── src/main/resources/
│   │   ├── db/migration/          # Flyway migrations V1-V9
│   │   ├── static/
│   │   │   └── dashboard.html     # Live trading dashboard
│   │   ├── application.yml
│   │   └── application-docker.yml
│   ├── src/test/java/
│   │   └── engine/
│   │       └── MatchingEngineTest.java    # 7 unit tests
│   └── Dockerfile
├── k8s/                           # Kubernetes manifests
│   ├── namespace.yml
│   ├── configmap.yml
│   ├── secret.yml
│   ├── deployment.yml
│   ├── service.yml
│   ├── ingress.yml
│   └── hpa.yml
├── .github/
│   └── workflows/
│       └── ci-cd.yml              # GitHub Actions pipeline
├── docker-compose.yml
└── prometheus.yml
```

---

## 🎯 Design Decisions

### Why Modular Monolith over Microservices?

The system is built as a modular monolith with clean bounded context boundaries (engine, API, market data, shared). This was intentional:

1. **Operational simplicity** — single deployment unit, no network latency between modules
2. **Extractable** — package boundaries match microservice boundaries; splitting is a `pom.xml` change
3. **Performance** — engine → publisher event emission is in-memory, not HTTP

### Why Single-Threaded Matching Engine?

The matching engine runs one thread per instrument. This eliminates:
- Lock contention between competing threads
- Race conditions in order book modification
- Non-deterministic ordering of simultaneous events

Scalability is achieved by sharding horizontally — add instruments, not threads.

### Why 202 Accepted for Order Placement?

Order matching is asynchronous. The HTTP thread validates the order and publishes to the Disruptor ring buffer, then returns immediately. The client receives a fill notification via WebSocket when the order is matched. This mirrors real exchange behavior (Binance, Zerodha).

### Why Kafka for Trade Events?

Trade events need to be processed by multiple consumers:
- Trade persistence consumer (PostgreSQL)
- Balance settlement consumer
- WebSocket market data publisher

Kafka allows all three to consume independently, retry on failure, and replay events for audit/recovery.

---

## 💬 Interview Talking Points

### The Question You'll Get: "Walk me through your project"

> "I built a real-time order matching engine — the core of a trading exchange. Orders come in via REST, pass through JWT auth and balance validation, then get published to an LMAX Disruptor ring buffer. A dedicated matching thread runs price-time priority on an in-memory TreeMap order book. Trades publish to Kafka, get persisted to PostgreSQL, balances settle, and real-time updates push to WebSocket clients. End-to-end from HTTP request to WebSocket notification takes under 10 milliseconds locally."

### Follow-up: "Why not use a database for the order book?"

> "The matching engine needs sub-millisecond latency. A database round-trip adds 1-10ms minimum — that's unacceptable for a matching loop. The order book is entirely in-memory (TreeMap) and event-sourced via Kafka. On restart, the engine replays open orders from PostgreSQL. The database is for persistence and audit, not for the matching critical path."

### Follow-up: "How do you handle concurrent orders?"

> "Two layers. First, the Disruptor ring buffer serializes all orders into a single-threaded sequence per instrument — no concurrent modification of the order book is possible. Second, balance reservation uses PostgreSQL pessimistic locking with an optimistic lock version column as backup. The DB CHECK constraint is the final safety net — `available_balance >= 0` is enforced at the database level regardless of application logic."

---

## 📄 License

MIT License — see [LICENSE](LICENSE) file.

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/your-feature`)
3. Commit changes (`git commit -m 'Add your feature'`)
4. Push to branch (`git push origin feature/your-feature`)
5. Open a Pull Request

---

<div align="center">
  <strong>Built with ❤️ as a production-grade portfolio project</strong><br/>
  <em>Demonstrating real-world Spring Boot architecture patterns</em>
</div>