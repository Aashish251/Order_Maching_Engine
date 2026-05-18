# Order Matching Engine

A Spring Boot 3 / Java 21 order matching engine for crypto-style markets. The service accepts authenticated orders, reserves account balances, routes commands through an LMAX Disruptor pipeline, matches orders per instrument, persists orders/trades in PostgreSQL, publishes trade events through Kafka, and streams market data over WebSocket/STOMP.

The repository also includes local infrastructure with Docker Compose, Prometheus scraping, a static realtime dashboard, Kubernetes manifests, and a GitHub Actions CI/CD pipeline.

## Contents

- [Features](#features)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Repository Layout](#repository-layout)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [Database Migrations](#database-migrations)
- [REST API](#rest-api)
- [Realtime Market Data](#realtime-market-data)
- [Dashboard](#dashboard)
- [Observability](#observability)
- [Testing](#testing)
- [Docker](#docker)
- [Kubernetes](#kubernetes)
- [CI/CD](#cicd)
- [Troubleshooting](#troubleshooting)

## Features

- JWT-based registration and login.
- Role-aware authorization for traders, market makers, and admins.
- Limit and market order support.
- Buy/sell balance reservation before order submission.
- Per-symbol in-memory matching engines.
- Disruptor-backed command pipeline for low-latency order submission.
- PostgreSQL persistence with Flyway schema migrations.
- Redis-backed caching for instruments and account-related hot paths.
- Kafka trade event publishing and consuming.
- WebSocket/STOMP market data streams for order book, trades, and user fills.
- Request logging, custom metrics, Prometheus endpoint, Zipkin tracing, and health probes.
- Docker Compose for local PostgreSQL, Redis, Kafka, Zipkin, Prometheus, Grafana, and admin UIs.
- Kubernetes deployment, service, config, secret, ingress, and HPA manifests.

## Architecture

```text
Client / Dashboard
       |
       | REST + JWT
       v
Spring Boot API
       |
       | validate, authorize, reserve balances
       v
Order Service
       |
       | command event
       v
LMAX Disruptor
       |
       v
Matching Engine Registry
       |
       | per-symbol order books
       v
Matching Engine
       |
       +--> PostgreSQL: users, accounts, instruments, orders, trades, trade legs
       +--> Kafka: trade.executed and related events
       +--> Redis: cache and market-data fanout support
       +--> WebSocket/STOMP: order book, trades, user order updates
```

The application starts matching engines for configured instruments:

- `BTC-USD`
- `ETH-USD`
- `SOL-USD`

These instruments are also seeded by Flyway.

## Technology Stack

| Area | Technology |
| --- | --- |
| Runtime | Java 21 |
| Framework | Spring Boot 3.3.0 |
| API | Spring Web, Spring Validation |
| Security | Spring Security, JWT |
| Persistence | Spring Data JPA, PostgreSQL |
| Migrations | Flyway |
| Cache / PubSub | Redis |
| Messaging | Kafka |
| Matching pipeline | LMAX Disruptor |
| Resilience | Resilience4j circuit breaker and retry |
| Observability | Spring Actuator, Micrometer, Prometheus, Zipkin |
| Tests | JUnit, Spring Boot Test, Testcontainers |
| Deployment | Docker, Kubernetes, GitHub Actions |

## Repository Layout

```text
.
|-- README.md
|-- docker-compose.yml
|-- prometheus.yml
|-- k8s/
|   |-- namespace.yml
|   |-- configmap.yml
|   |-- secret.yml
|   |-- deployment.yml
|   |-- service.yml
|   |-- ingress.yml
|   `-- hpa.yml
`-- ome/
    |-- pom.xml
    |-- Dockerfile
    |-- src/main/java/com/project/ome/
    |   |-- api/
    |   |-- engine/
    |   |-- marketdata/
    |   |-- publisher/
    |   `-- shared/
    |-- src/main/resources/
    |   |-- application.yml
    |   |-- application-docker.yml
    |   |-- static/dashboard.html
    |   `-- db/migration/
    `-- src/test/java/com/project/ome/
```

## Prerequisites

- Java 21
- Maven 3.9+ or the included Maven wrapper
- Docker and Docker Compose
- Git
- Optional: `kubectl` for Kubernetes deployment

## Quick Start

Start the local infrastructure:

```bash
docker compose up -d
```

Run the Spring Boot application:

```bash
cd ome
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
cd ome
.\mvnw.cmd spring-boot:run
```

Check that the service is up:

```bash
curl http://localhost:8080/api/v1/ping
```

Expected response:

```json
{
  "success": true,
  "message": null,
  "data": {
    "status": "UP",
    "service": "Order Matching Engine",
    "version": "1.0.0"
  },
  "error": null
}
```

Useful local URLs:

| Service | URL | Notes |
| --- | --- | --- |
| Application | `http://localhost:8080` | Spring Boot app |
| Dashboard | `http://localhost:8080/dashboard.html` | Static realtime dashboard |
| Actuator health | `http://localhost:8080/actuator/health` | Liveness/readiness details |
| Prometheus metrics | `http://localhost:8080/actuator/prometheus` | Scraped by Prometheus |
| pgAdmin | `http://localhost:5050` | `admin@ome.com` / `admin` |
| Redis Insight | `http://localhost:8001` | Redis UI |
| Kafka UI | `http://localhost:8090` | Kafka topics and messages |
| Zipkin | `http://localhost:9411` | Distributed traces |
| Prometheus | `http://localhost:9090` | Metrics backend |
| Grafana | `http://localhost:3000` | `admin` / `admin` |

## Configuration

Main configuration lives in `ome/src/main/resources/application.yml`.

Important environment variables:

| Variable | Default | Description |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/ome_db` | PostgreSQL JDBC URL |
| `DB_USERNAME` | `ome_user` | PostgreSQL username |
| `DB_PASSWORD` | `ome_pass` | PostgreSQL password |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |
| `REDIS_PASSWORD` | `ome_redis_pass` | Redis password |
| `KAFKA_SERVERS` | `localhost:9092` | Kafka bootstrap servers |
| `JWT_SECRET` | development secret | JWT signing secret |
| `SPRING_SECURITY_PASSWORD` | `admin` | Default Spring security password if needed |

The Docker profile overrides service hosts in `application-docker.yml`:

- PostgreSQL: `postgres:5432`
- Redis: `redis:6379`
- Kafka: `kafka:29092`
- Zipkin: `zipkin:9411`

## Database Migrations

Flyway runs automatically at application startup. Migrations are stored in:

```text
ome/src/main/resources/db/migration
```

Current migrations create:

- users
- accounts
- instruments
- orders
- trades
- trade legs
- seed instruments
- missing audit columns

Seeded instruments:

| Symbol | Base | Quote | Tick Size | Lot Size | Minimum Order Value |
| --- | --- | --- | --- | --- | --- |
| `BTC-USD` | BTC | USD | `0.01` | `0.00001` | `10` |
| `ETH-USD` | ETH | USD | `0.01` | `0.0001` | `5` |
| `SOL-USD` | SOL | USD | `0.001` | `0.01` | `1` |

Newly registered users are created with demo balances for local testing:

- `100000.00 USD`
- `10.00 BTC`
- `100.00 ETH`

## REST API

Base URL:

```text
http://localhost:8080
```

Most endpoints return the shared `ApiResponse` shape:

```json
{
  "success": true,
  "message": "Operation message",
  "data": {},
  "error": null
}
```

### Health

```http
GET /api/v1/ping
```

This endpoint is public.

### Register

```http
POST /api/v1/auth/register
Content-Type: application/json
```

```json
{
  "username": "alice",
  "email": "alice@example.com",
  "password": "password123"
}
```

Response data includes:

```json
{
  "accessToken": "jwt-token",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "role": "ROLE_TRADER"
}
```

Registration creates a `ROLE_TRADER` user and seeds demo accounts.

### Login

```http
POST /api/v1/auth/login
Content-Type: application/json
```

```json
{
  "email": "alice@example.com",
  "password": "password123"
}
```

Use the returned JWT on protected endpoints:

```http
Authorization: Bearer <accessToken>
```

Login is rate limited by client IP.

### Place Order

```http
POST /api/v1/orders
Authorization: Bearer <accessToken>
Content-Type: application/json
```

Limit buy example:

```json
{
  "symbol": "BTC-USD",
  "side": "BUY",
  "type": "LIMIT",
  "quantity": 0.01,
  "price": 50000.00,
  "clientOrderId": "alice-btc-buy-001"
}
```

Limit sell example:

```json
{
  "symbol": "BTC-USD",
  "side": "SELL",
  "type": "LIMIT",
  "quantity": 0.01,
  "price": 51000.00,
  "clientOrderId": "alice-btc-sell-001"
}
```

Market order example:

```json
{
  "symbol": "BTC-USD",
  "side": "BUY",
  "type": "MARKET",
  "quantity": 0.01,
  "clientOrderId": "alice-btc-market-001"
}
```

Notes:

- `side` must be `BUY` or `SELL`.
- `type` must be `LIMIT` or `MARKET`.
- Limit orders require `price`.
- Market buy orders require available ask liquidity because the service estimates cost from the best ask plus slippage.
- `clientOrderId` is optional but enables idempotent order placement.
- Required role: `ROLE_TRADER` or `ROLE_MARKET_MAKER`.

### Get Orders

```http
GET /api/v1/orders?page=0&size=20
Authorization: Bearer <accessToken>
```

Returns the authenticated user's orders in reverse creation order.

### Get Order By ID

```http
GET /api/v1/orders/{orderId}
Authorization: Bearer <accessToken>
```

Users can only fetch their own orders.

### Cancel Order

```http
DELETE /api/v1/orders/{orderId}
Authorization: Bearer <accessToken>
```

Required role: `ROLE_TRADER` or `ROLE_MARKET_MAKER`.

The service releases reserved balances and submits a cancel command to the matching engine.

### Admin Cache APIs

Admin endpoints require `ROLE_ADMIN`.

```http
POST /api/v1/admin/cache/instruments/evict/{symbol}
Authorization: Bearer <adminAccessToken>
```

```http
POST /api/v1/admin/cache/instruments/warmup
Authorization: Bearer <adminAccessToken>
```

The public registration flow creates trader users. For local admin testing, promote a user in the database or seed an admin account.

## Realtime Market Data

WebSocket endpoint:

```text
http://localhost:8080/ws
```

SockJS is enabled for browser clients. Native WebSocket clients can also connect to `/ws`.

The dashboard passes the token as a query parameter:

```text
http://localhost:8080/ws?token=<jwt>
```

It also sends the STOMP header:

```text
Authorization: Bearer <jwt>
```

Public subscriptions:

| Topic | Description |
| --- | --- |
| `/topic/{symbol}/orderbook` | Live order book snapshots/updates |
| `/topic/{symbol}/trades` | Live trade feed |

Private subscriptions:

| Topic | Description |
| --- | --- |
| `/user/queue/orders` | Authenticated user's order updates and fills |

Example topics for BTC:

```text
/topic/BTC-USD/orderbook
/topic/BTC-USD/trades
/user/queue/orders
```

## Dashboard

A static dashboard is served from:

```text
http://localhost:8080/dashboard.html
```

The dashboard supports:

- JWT input.
- Symbol selection.
- Order book stream.
- Trade feed stream.
- Private fill notifications when authenticated.
- Connection status and activity log.

Recommended local flow:

1. Start infrastructure with `docker compose up -d`.
2. Start the app from `ome/`.
3. Register or login through the REST API.
4. Paste the returned access token into the dashboard.
5. Connect to `BTC-USD`, `ETH-USD`, or `SOL-USD`.
6. Place orders through the REST API and watch updates stream into the dashboard.

## Observability

Spring Actuator exposes:

| Endpoint | Description |
| --- | --- |
| `/actuator/health` | Health details |
| `/actuator/health/liveness` | Kubernetes liveness probe |
| `/actuator/health/readiness` | Kubernetes readiness probe |
| `/actuator/prometheus` | Prometheus metrics |
| `/actuator/metrics` | Metrics browser |
| `/actuator/circuitbreakers` | Resilience4j circuit breaker state |
| `/actuator/loggers` | Runtime logger configuration |

Prometheus is configured by `prometheus.yml` to scrape:

```text
host.docker.internal:8080/actuator/prometheus
```

This matches the default local setup where infrastructure runs in Docker and the Spring app runs on the host.

Zipkin tracing is enabled with 100% sampling for local development. Reduce sampling for production workloads.

## Testing

Run tests from the `ome` directory:

```bash
cd ome
./mvnw test
```

Run only the matching engine tests:

```bash
cd ome
./mvnw test -Dtest=MatchingEngineTest
```

Run the full Maven verification lifecycle:

```bash
cd ome
./mvnw verify
```

On Windows PowerShell, replace `./mvnw` with `.\mvnw.cmd`.

## Docker

Start local dependencies:

```bash
docker compose up -d
```

Stop local dependencies:

```bash
docker compose down
```

Remove local dependency volumes:

```bash
docker compose down -v
```

Build the application image:

```bash
docker build -t ome:local ./ome
```

Run the application image on the Docker network created by Compose:

```bash
docker run --rm \
  --name ome_app \
  --network order_maching_engine_default \
  -p 8080:8080 \
  ome:local
```

If your Compose project name differs, inspect networks with:

```bash
docker network ls
```

The application image uses the `docker` Spring profile by default.

## Kubernetes

Kubernetes manifests are stored in `k8s/`.

Apply the manifests:

```bash
kubectl apply -f k8s/namespace.yml
kubectl apply -f k8s/configmap.yml
kubectl apply -f k8s/secret.yml
kubectl apply -f k8s/deployment.yml
kubectl apply -f k8s/service.yml
kubectl apply -f k8s/hpa.yml
kubectl apply -f k8s/ingress.yml
```

Check rollout:

```bash
kubectl rollout status deployment/ome-app -n ome
```

Check pods:

```bash
kubectl get pods -n ome
```

Port-forward locally:

```bash
kubectl port-forward -n ome service/ome-service 8080:80
```

Important production notes:

- Replace `ghcr.io/YOUR_GITHUB_USERNAME/ome:latest` in `k8s/deployment.yml`.
- Replace secrets in `k8s/secret.yml`.
- Ensure PostgreSQL, Redis, and Kafka services exist in the cluster or update `k8s/configmap.yml`.
- Tighten WebSocket allowed origins before exposing publicly.

## CI/CD

The GitHub Actions workflow is defined in:

```text
.github/workflows/ci-cd.yml
```

Pipeline stages:

1. Build and test with Java 21.
2. Run matching engine tests and full verification.
3. Build and push Docker image to GitHub Container Registry on `main`.
4. Deploy to Kubernetes on `main`.
5. Smoke test the deployed pod.

Required deployment secret:

| Secret | Description |
| --- | --- |
| `KUBE_CONFIG` | Base64-encoded kubeconfig used by the deploy job |

## Troubleshooting

### Application cannot connect to PostgreSQL

Confirm Compose services are running:

```bash
docker compose ps
```

Check the configured database URL:

```text
jdbc:postgresql://localhost:5432/ome_db
```

Default credentials:

```text
username: ome_user
password: ome_pass
database: ome_db
```

### Redis authentication fails

Redis is started with:

```text
password: ome_redis_pass
```

Make sure `REDIS_PASSWORD` matches.

### Kafka connection fails locally

When running the Spring app on the host, use:

```text
KAFKA_SERVERS=localhost:9092
```

When running the app inside Docker on the Compose network, use:

```text
KAFKA_SERVERS=kafka:29092
```

### Market buy returns `NO_LIQUIDITY`

Market buy orders need an existing ask in the matching engine order book. Place a sell limit order first, then submit the market buy.

### Order placement returns insufficient balance

The registration flow seeds demo balances. If you created users manually, add account rows for the required currencies or register through `/api/v1/auth/register`.

### Dashboard connects but receives no private fills

Private fills require a valid JWT. Paste the raw JWT access token into the dashboard token field; the dashboard adds the `Bearer` prefix when connecting.

### Admin endpoints return forbidden

Registered users are created with `ROLE_TRADER`. Promote a user to `ROLE_ADMIN` in the database for local admin testing.

## Security Notes

This project includes development defaults for local use. Before production deployment:

- Replace all default passwords.
- Provide a strong `JWT_SECRET`.
- Store secrets in a secure secret manager.
- Restrict WebSocket allowed origins.
- Review rate limit values.
- Reduce tracing sample rate.
- Review exposed actuator endpoints.
- Replace the Kubernetes image placeholder.
