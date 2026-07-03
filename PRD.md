# Product Requirements Document — Food Delivery System (Swiggy-Style)

## 1. Project Overview

**Product**: A web-based food delivery platform connecting customers with restaurants and delivery partners.  
**Model**: Restaurant-to-customer (mirroring Swiggy).  
**Scale**: MVP with production-ready microservices architecture.  
**Team**: Solo developer.  
**Repository**: Gradle multi-module monorepo.

---

## 2. Tech Stack

| Layer              | Technology                                                                 |
|--------------------|----------------------------------------------------------------------------|
| Backend Framework  | Spring Boot 3.x (Java 21)                                                  |
| Build Tool         | Gradle (Kotlin DSL, multi-module monorepo)                                 |
| API Gateway        | Spring Cloud Gateway                                                       |
| Service Discovery  | Spring Cloud Netflix Eureka                                                |
| Config Management  | Spring Cloud Config Server                                                 |
| Database           | PostgreSQL 16 (separate DB per microservice)                               |
| Caching            | Redis 7                                                                   |
| Messaging          | RabbitMQ (for async inter-service communication)                           |
| Authentication     | Keycloak 24+ (OAuth2 + OpenID Connect) with JWT tokens                     |
| Containerization   | Docker + Docker Compose                                                    |
| CI/CD              | GitHub Actions                                                             |
| Monitoring         | Prometheus + Grafana (Micrometer + Actuator)                               |
| Distributed Tracing| Micrometer Tracing + Zipkin                                                |
| Frontend           | React 18+ with TypeScript, Vite, React Router, Redux Toolkit / Zustand     |
| Payment Gateway    | Razorpay (primary), Stripe (secondary)                                     |

---

## 3. Microservices Architecture

### 3.1 Service Inventory (8 Services)

| # | Service              | Responsibility                                                                            |
|---|----------------------|-------------------------------------------------------------------------------------------|
| 1 | **User Service**      | Registration, login, profiles, addresses, roles (customer/delivery partner/restaurant/admin), loyalty subscription |
| 2 | **Restaurant Service**| Restaurant onboarding, menu management (items, categories, pricing), operating hours, availability |
| 3 | **Cart Service**      | Cart creation, item add/remove/modify, coupon application, cart expiry (TTL via Redis)    |
| 4 | **Order Service**     | Order creation, status lifecycle, order history, cancellation/refund workflows            |
| 5 | **Delivery Service**  | Delivery partner onboarding, real-time GPS tracking, automatic assignment engine, ETA calculation |
| 6 | **Payment Service**   | Payment initiation, gateway integration (Razorpay/Stripe), payment verification, refunds  |
| 7 | **Notification Service** | In-app notification dispatch (order status, delivery updates, promotions)              |
| 8 | **Search/Catalog Service** | Restaurant search (name, cuisine, location), menu search, geo-spatial queries (PostGIS) |

### 3.2 Communication Patterns

```
                    ┌─────────────────┐
                    │  API Gateway     │
                    │ (Spring Cloud    │
                    │   Gateway)        │
                    └───────┬─────────┘
                            │
                ┌───────────┼───────────┐
                ▼           ▼           ▼
        ┌──────────┐ ┌──────────┐ ┌──────────┐
        │  User    │ │Search/   │ │  Cart    │
        │ Service  │ │Catalog   │ │ Service  │
        └────┬─────┘ └────┬─────┘ └────┬─────┘
             │            │            │
             └────────────┼────────────┘
                          │ (REST sync)
                          │
        ┌─────────────────┼─────────────────┐
        ▼                 ▼                 ▼
  ┌──────────┐    ┌──────────┐     ┌──────────────┐
  │ Order    │◄──►│ Delivery │     │  Payment     │
  │ Service  │    │ Service  │     │  Service     │
  └────┬─────┘    └────┬─────┘     └──────┬───────┘
       │               │                  │
       └───────────────┼──────────────────┘
                       │
                       ▼
              ┌────────────────┐
              │ Notification   │
              │ Service        │
              └────────────────┘

Sync (REST/Feign): Gateway ↔ Services, Cart → Restaurant (menu validation)
Async (RabbitMQ): Order → Delivery (assign partner), Order → Payment (charge), Order → Notification (status updates), Delivery → Notification (tracking updates), Payment → Order (confirmation)
```

### 3.3 API Routes (Gateway Level)

```
/users/**            → User Service        (PORT 8081)
/restaurants/**      → Restaurant Service  (PORT 8082)
/cart/**             → Cart Service        (PORT 8083)
/orders/**           → Order Service       (PORT 8084)
/delivery/**         → Delivery Service    (PORT 8085)
/payments/**         → Payment Service     (PORT 8086)
/notifications/**    → Notification Service(PORT 8087)
/search/**           → Search/Catalog Svc  (PORT 8088)

/auth/**             → Keycloak proxy
```

---

## 4. Database Design (Per Service)

### 4.1 User Service DB

```
users (id, email, phone, password_hash, full_name, role, created_at, updated_at)
user_addresses (id, user_id, label, address_line1, address_line2, city, state, pincode, latitude, longitude, is_default, created_at)
loyalty_subscriptions (id, user_id, plan_type, status, start_date, end_date, auto_renew, created_at)
```

### 4.2 Restaurant Service DB

```
restaurants (id, owner_id, name, description, cuisine_type, address_line1, address_line2, city, state, pincode, latitude, longitude, rating, is_active, opening_time, closing_time, created_at, updated_at)
menu_categories (id, restaurant_id, name, display_order, created_at)
menu_items (id, restaurant_id, category_id, name, description, price, image_url, is_vegetarian, is_available, dietary_tags, created_at, updated_at)
```

### 4.3 Cart Service DB (mostly Redis — PostgreSQL for persistence/analytics)

```
Redis key pattern: cart:{userId} → JSON { restaurantId, items: [{itemId, qty, unitPrice}], couponCode, metadata }
TTL: 30 minutes of inactivity
```

### 4.4 Order Service DB

```
orders (id, user_id, restaurant_id, delivery_address_id, status, subtotal, delivery_fee, tax, discount, total_amount, payment_status, payment_id, created_at, updated_at)
order_items (id, order_id, menu_item_id, item_name, quantity, unit_price, total_price)
order_status_log (id, order_id, status, timestamp, note)
```

### 4.5 Delivery Service DB

```
delivery_partners (id, user_id, vehicle_type, license_number, is_verified, is_online, current_latitude, current_longitude, average_rating, created_at)
delivery_assignments (id, order_id, partner_id, status, assigned_at, picked_up_at, delivered_at)
delivery_locations (id, assignment_id, latitude, longitude, timestamp)
```

### 4.6 Payment Service DB

```
payments (id, order_id, user_id, amount, gateway, gateway_payment_id, gateway_order_id, status, method, created_at, updated_at)
payment_transactions (id, payment_id, type, gateway_response, created_at)
refunds (id, payment_id, amount, reason, status, gateway_refund_id, created_at)
```

### 4.7 Notification Service DB

```
notifications (id, user_id, type, title, body, data_json, is_read, created_at)
notification_preferences (id, user_id, order_updates, promotions, delivery_updates)
```

### 4.8 Search/Catalog Service DB (PostgreSQL + PostGIS extension)

```
Uses PostGIS for geo-spatial queries
Indexes on restaurants(name, cuisine_type, city) and menu_items(name)
Materialized views for frequently accessed search results
```

---

## 5. Key Feature Flows

### 5.1 User Registration & Login

1. User registers via React UI → Gateway → User Service creates account
2. User Service syncs user to Keycloak for OAuth2 identity
3. Login returns JWT access token + refresh token
4. Subsequent requests carry `Authorization: Bearer <JWT>` through Gateway
5. Each downstream service validates JWT signature and extracts roles

### 5.2 Restaurant Search & Menu Browsing

1. Customer searches by cuisine/city/restaurant name → Search Service
2. Search Service uses PostGIS for location-based results (sort by proximity + rating)
3. Customer views menu → Restaurant Service returns categories + items
4. Redis caches frequently queried restaurant menus (TTL: 15 min)

### 5.3 Cart & Checkout

1. Customer adds items → Cart Service validates against Restaurant Service (item exists, available)
2. Cart keyed to user in Redis with 30-min TTL
3. Cart enforces "single restaurant only" — adding from another restaurant clears cart with confirmation
4. Coupon codes validated against predefined rules in Cart Service
5. Checkout: Cart Service calculates totals → sends to Order Service

### 5.4 Order Creation

1. Customer confirms checkout → Order Service creates order (status: PLACED)
2. Order Service publishes `order.placed` event to RabbitMQ
3. Payment Service consumes event → creates payment → redirects to Razorpay/Stripe checkout
4. Payment confirmation publishes `payment.completed` → Order Service updates payment_status
5. Delivery Service consumes `order.placed` → runs automatic assignment engine

### 5.5 Automatic Delivery Assignment

1. Delivery Service receives `order.placed` event
2. Assignment engine queries available partners (online, not busy) within X km of restaurant
3. Sorts by: proximity to restaurant, current load, rating
4. Assigns best match → publishes `delivery.assigned`
5. Notification Service sends in-app alert to both customer and delivery partner
6. If no partner found, retries with expanding radius every 60 seconds (up to 3 retries, then flag for manual)

### 5.6 Real-Time GPS Tracking

1. Delivery partner's device sends location pings every 5 seconds (while on delivery)
2. Delivery Service stores location in `delivery_locations` table
3. Frontend polls `/delivery/orders/{orderId}/tracking` every 5 seconds
4. Response includes partner's current lat/lng + ETA
5. (MVP: HTTP polling. Future enhancement: WebSocket/SSE)

### 5.7 Loyalty Subscription

1. User subscribes → User Service creates `loyalty_subscriptions` record
2. Subscription benefits: free delivery on orders above ₹X, exclusive discounts
3. Order Service checks `GET /users/{id}/subscription` before applying delivery fee
4. Auto-renewal handled by Payment Service (scheduled job)

### 5.8 In-App Notifications

- Notification Service exposes REST endpoint for UI to fetch unread notifications
- Frontend polls `/notifications/unread` periodically (or shows badge count)
- Events trigger notification creation: order confirmed, partner assigned, delivery approaching, payment success/failure, promotional
- Read status toggled via PATCH endpoint

---

## 6. Security

| Area             | Approach                                                                     |
|------------------|-----------------------------------------------------------------------------|
| Authentication   | Keycloak (OAuth2 + OpenID Connect), JWT tokens                              |
| Authorization    | Role-based (CUSTOMER, RESTAURANT_OWNER, DELIVERY_PARTNER, ADMIN) via JWT claims |
| Inter-service    | Mutual TLS (mTLS) or JWT propagation (service-to-service tokens)            |
| API Gateway      | Rate limiting, CORS, request validation, CSRF protection                    |
| Secrets          | Environment variables / Docker secrets (no hardcoded credentials)           |
| Data at rest     | PostgreSQL encryption (pgcrypto for PII)                                    |
| Data in transit  | TLS (HTTPS)                                                                 |
| Payment data     | Never stored — handled entirely by Razorpay/Stripe (PCI-DSS outsourced)     |

---

## 7. Docker Compose Infrastructure

### Services (Containers)

```
postgres-users        postgres:16-alpine
postgres-restaurants  postgres:16-alpine
postgres-orders       postgres:16-alpine
postgres-delivery     postgres:16-alpine
postgres-payments     postgres:16-alpine
postgres-notifications postgres:16-alpine
postgres-search       postgres:16-alpine + PostGIS
redis-cache           redis:7-alpine
rabbitmq              rabbitmq:3-management-alpine
keycloak              quay.io/keycloak/keycloak:24
zipkin                openzipkin/zipkin
prometheus            prom/prometheus
grafana               grafana/grafana
eureka-server         (custom Spring Boot image)
config-server         (custom Spring Boot image)
gateway-service       (custom Spring Boot image)
user-service          (custom Spring Boot image)
restaurant-service    (custom Spring Boot image)
cart-service          (custom Spring Boot image)
order-service         (custom Spring Boot image)
delivery-service      (custom Spring Boot image)
payment-service       (custom Spring Boot image)
notification-service  (custom Spring Boot image)
search-service        (custom Spring Boot image)
react-frontend        node:20-alpine (dev) / nginx:alpine (prod)
```

~25 containers total for MVP. Docker Compose handles networking (single bridge network), health checks, and startup ordering.

---

## 8. Project Structure (Monorepo)

```
food-delivery/
├── build.gradle.kts              # Root build (all projects, common plugins)
├── settings.gradle.kts           # Include all submodules
├── gradle.properties
├── gradle/
│   └── libs.versions.toml        # Version catalog
├── docker-compose.yml            # Local dev environment
├── docker-compose.infrastructure.yml  # DBs, cache, broker, keycloak only
├── .github/
│   └── workflows/
│       ├── build.yml             # Build + test on PR/push
│       └── deploy.yml            # Optional deployment
├── common/
│   ├── common-lib/               # Shared DTOs, exceptions, utilities
│   └── common-config/            # Shared Spring config (JWT validation, Feign config)
├── infrastructure/
│   ├── eureka-server/
│   ├── config-server/
│   └── gateway-service/
├── services/
│   ├── user-service/
│   ├── restaurant-service/
│   ├── cart-service/
│   ├── order-service/
│   ├── delivery-service/
│   ├── payment-service/
│   ├── notification-service/
│   └── search-service/
├── frontend/
│   └── react-app/
└── docs/
    ├── PRD.md
    ├── architecture.md
    └── api-specs/
```

---

## 9. CI/CD Pipeline (GitHub Actions)

```yaml
Trigger: push to main, PR to main

Jobs:
  1. Build & Unit Test → All Gradle modules
  2. Code Quality → SonarQube / SpotBugs
  3. Docker Image Build → All services
  4. Integration Tests → Spin up Docker Compose, run contract tests
  5. (Optional) Push images to Docker Hub / GHCR

Branch protection: Require passing CI before merge
```

---

## 10. Monitoring & Observability

| Component      | Tool                        | What It Captures                                      |
|----------------|-----------------------------|-------------------------------------------------------|
| Metrics        | Prometheus + Micrometer     | JVM stats, request latency, throughput, error rates   |
| Dashboards     | Grafana                     | Service health, business KPIs (orders/min, delivery time) |
| Tracing        | Zipkin (Micrometer Tracing) | Distributed trace across service calls                |
| Health Checks  | Spring Boot Actuator        | `/actuator/health`, `/actuator/metrics`               |
| Alerts         | Grafana Alerting            | Service down, high error rate, DB connection failures |

---

## 11. Development Phases (MVP)

Each checkbox below is a single commit — focused, reviewable, and shippable independently where possible.

---

### Phase 1 — Foundation (13 commits)

#### 1.1 Project Skeleton
- [ ] **P1.1.1** Root Gradle multi-module project with Kotlin DSL, version catalog (`libs.versions.toml`), `settings.gradle.kts`, `.gitignore`
- [ ] **P1.1.2** `common-lib` module — shared DTOs base, custom exceptions (`NotFoundException`, `BadRequestException`), `ApiResponse<T>` wrapper
- [ ] **P1.1.3** `common-config` module — shared Spring Boot config base (JWT resource server, Feign error decoder, Jackson config)
- [ ] **P1.1.4** `docker-compose.infrastructure.yml` — PostgreSQL (7 instances), Redis, RabbitMQ, Keycloak, Zipkin, Prometheus, Grafana. Each DB with separate volume, health checks, startup ordering
- [ ] **P1.1.5** `docker-compose.yml` — full local dev compose (includes infra + all service stubs)

#### 1.2 Infrastructure Services
- [ ] **P1.2.1** Eureka Server — Spring Cloud Netflix Eureka with basic auth, standalone runnable
- [ ] **P1.2.2** Config Server — Spring Cloud Config reading from classpath / local filesystem, connected to Eureka
- [ ] **P1.2.3** API Gateway — Spring Cloud Gateway with Eureka discovery, CORS config, actuator. Route all service paths. No auth yet

#### 1.3 User Service
- [ ] **P1.3.1** User Service skeleton — Spring Boot project, PostgreSQL config, Flyway migration for `users` + `user_addresses` tables, Eureka client registration
- [ ] **P1.3.2** User registration + login endpoints (`POST /register`, `POST /login`), password hashing (BCrypt), basic DTO validation
- [ ] **P1.3.3** Keycloak integration — realm config JSON, Keycloak client setup, Keycloak admin client in User Service to sync users, Gateway JWT resource server config
- [ ] **P1.3.4** User profile CRUD (`GET/PUT /users/{id}`), address management (`GET/POST/DELETE /users/addresses`)

#### 1.4 Frontend Scaffolding
- [ ] **P1.4.1** React app — Vite + TypeScript project, React Router (Home, Login, Register pages), Tailwind CSS, Axios instance with base URL + JWT interceptor, Keycloak JS adapter for token management

---

### Phase 2 — Core Domain (14 commits)

#### 2.1 Restaurant Service
- [ ] **P2.1.1** Restaurant Service skeleton — Spring Boot project, PostgreSQL config, Flyway migration for `restaurants` + `menu_categories` + `menu_items` tables, Eureka client
- [ ] **P2.1.2** Restaurant CRUD — create, get by ID, update restaurant (owner-only access)
- [ ] **P2.1.3** Menu management — create/list categories, add/update items, toggle item availability, get full menu by restaurant ID
- [ ] **P2.1.4** Restaurant search (basic) — search by name, cuisine type, city. Cached with Redis (TTL 15 min). Internal endpoint consumed by Search Service later

#### 2.2 Search/Catalog Service
- [ ] **P2.2.1** Search Service skeleton — Spring Boot project, PostgreSQL + PostGIS config, Flyway migration, PostGIS extension enablement, Eureka client
- [ ] **P2.2.2** Geo-spatial restaurant search — `GET /search/restaurants` with lat/lng/radius, cuisine filter, pagination. PostGIS `ST_DWithin` queries. Results sorted by proximity + rating
- [ ] **P2.2.3** Menu search — `GET /search/menu` by restaurant ID + query string + vegetarian filter. Full-text search on item names
- [ ] **P2.2.4** Restaurant data sync — Search Service consumes `restaurant.created`/`restaurant.updated` events via RabbitMQ to keep its read model in sync

#### 2.3 Cart Service
- [ ] **P2.3.1** Cart Service skeleton — Spring Boot project, Redis config, Eureka client
- [ ] **P2.3.2** Cart CRUD in Redis — add item, update quantity, remove item, get cart, clear cart. Key pattern `cart:{userId}`. Single-restaurant enforcement (adding from different restaurant clears cart with warning)
- [ ] **P2.3.3** Cart item validation — on add, validate item exists + is available by calling Restaurant Service via Feign client. Handle restaurant-service-down gracefully
- [ ] **P2.3.4** Coupon management — apply/remove coupon, discount calculation, minimum order validation
- [ ] **P2.3.5** Cart auto-expiry — Redis TTL of 30 min on cart key, reset on any cart mutation
- [ ] **P2.3.6** Checkout endpoint — calculate totals (subtotal, delivery fee, tax, discount, total), return order preview before placement

---

### Phase 3 — Transactions (11 commits)

#### 3.1 Order Service
- [ ] **P3.1.1** Order Service skeleton — Spring Boot project, PostgreSQL config, Flyway migration for `orders` + `order_items` + `order_status_log` tables, Eureka client
- [ ] **P3.1.2** Place order — `POST /orders` reads cart from Cart Service, creates order with items, clears cart, publishes `order.placed` event to RabbitMQ
- [ ] **P3.1.3** Order status lifecycle — status transitions (PLACED → CONFIRMED → PREPARING → READY → PICKED_UP → DELIVERED, with CANCELLED from states before PICKED_UP). `order_status_log` audit entries
- [ ] **P3.1.4** Order endpoints — get order by ID, list user's orders with pagination, cancel order, get current status
- [ ] **P3.1.5** RabbitMQ consumers — listen to `payment.completed` (update payment status), `delivery.assigned` (update order status to CONFIRMED), `delivery.completed` (update order status to DELIVERED)

#### 3.2 Payment Service
- [ ] **P3.2.1** Payment Service skeleton — Spring Boot project, PostgreSQL config, Flyway migration for `payments` + `payment_transactions` + `refunds` tables, Eureka client
- [ ] **P3.2.2** Razorpay integration — create order, initiate payment, handle callback webhook (`POST /payments/callback/razorpay`), verify signature, update payment status, publish `payment.completed` event
- [ ] **P3.2.3** Stripe integration — create payment intent, handle webhook (`POST /payments/callback/stripe`), verify signature, update payment status, publish `payment.completed` event
- [ ] **P3.2.4** RabbitMQ consumer — listen to `order.placed`, auto-initiate payment with preferred gateway
- [ ] **P3.2.5** Refund flow — `POST /payments/{id}/refund`, gateway refund, publish `payment.refunded` event. Order Service listens to this to update order cancellation
- [ ] **P3.2.6** Payment status inquiry — `GET /payments/{id}`, payment history per user

---

### Phase 4 — Delivery + Notifications (11 commits)

#### 4.1 Delivery Service
- [ ] **P4.1.1** Delivery Service skeleton — Spring Boot project, PostgreSQL config, Flyway migration for `delivery_partners` + `delivery_assignments` + `delivery_locations` tables, Eureka client
- [ ] **P4.1.2** Delivery partner onboarding + management — register partner, verify documents, toggle online/offline status
- [ ] **P4.1.3** RabbitMQ consumer — listen to `order.placed`, trigger automatic assignment engine
- [ ] **P4.1.4** Automatic assignment engine — query online partners within radius of restaurant, sort by proximity + current load + rating, assign best match. Publish `delivery.assigned` event. Retry with expanding radius (3 attempts, 60s apart). Flag for manual if exhausted
- [ ] **P4.1.5** Delivery status lifecycle — ASSIGNED → ACCEPTED → PICKED_UP → DELIVERED, with REJECTED (re-trigger assignment). Update assignment status, publish `delivery.status.updated`
- [ ] **P4.1.6** Real-time GPS tracking — `POST /delivery/partners/{id}/location` (partner pings every 5s), `GET /delivery/orders/{orderId}/tracking` (returns current lat/lng + ETA). Store location history in `delivery_locations`

#### 4.2 Notification Service
- [ ] **P4.2.1** Notification Service skeleton — Spring Boot project, PostgreSQL config, Flyway migration for `notifications` + `notification_preferences` tables, Eureka client
- [ ] **P4.2.2** Create + store notification — internal endpoint to create notification, store in DB with type/title/body/read status
- [ ] **P4.2.3** RabbitMQ consumers — subscribe to `order.*`, `delivery.*`, `payment.*` events, create corresponding notifications
- [ ] **P4.2.4** Notification fetch + read — `GET /notifications` (paginated), `GET /notifications/unread/count`, `PATCH /notifications/{id}/read`
- [ ] **P4.2.5** Notification preferences — `GET/PUT /notifications/preferences`, filter notifications by user preferences

---

### Phase 5 — Loyalty, Monitoring, CI/CD, Polish (13 commits)

#### 5.1 Loyalty Subscription
- [ ] **P5.1.1** Loyalty DB — Flyway migration for `loyalty_subscriptions` table in User Service
- [ ] **P5.1.2** Subscribe / status endpoints — `POST /users/subscriptions`, `GET /users/{id}/subscription`. Plan types: MONTHLY, QUARTERLY, ANNUAL
- [ ] **P5.1.3** Order Service integration — check `GET /users/{id}/subscription` via Feign before applying delivery fee. Active subscription → free delivery on orders above threshold
- [ ] **P5.1.4** Auto-renewal — scheduled job checks expiring subscriptions, triggers payment via Payment Service

#### 5.2 Monitoring & Observability
- [ ] **P5.2.1** Micrometer + Prometheus — add Actuator + Micrometer dependencies to all services, expose `/actuator/prometheus`, configure Prometheus scrape jobs in Docker Compose
- [ ] **P5.2.2** Grafana dashboards — provision datasource (Prometheus), create dashboards: JVM health, service throughput/latency, error rates, order metrics per service
- [ ] **P5.2.3** Distributed tracing — add Micrometer Tracing + Zipkin Brave to all services, ensure trace IDs propagate across Feign + RabbitMQ calls

#### 5.3 CI/CD
- [ ] **P5.3.1** GitHub Actions — build workflow: check out, set up JDK 21 + Gradle cache, compile + test all modules, build Docker images for all services
- [ ] **P5.3.2** Integration test workflow — spin up Docker Compose, run contract/API tests against running services

#### 5.4 Frontend User Journey
- [ ] **P5.4.1** Restaurant search + listing page — search bar (cuisine, location), results grid with restaurant cards, pagination
- [ ] **P5.4.2** Restaurant menu page — categories accordion, item cards with add-to-cart, vegetarian filter, cart sidebar
- [ ] **P5.4.3** Cart + checkout page — cart review, coupon input, address selection, order summary, Razorpay/Stripe checkout redirect
- [ ] **P5.4.4** Order tracking page — live order status timeline, delivery partner GPS map (Leaflet/Google Maps), ETA display. Polling every 5s

---

### Commit Count Summary

| Phase   | Commits |
|---------|---------|
| Phase 1 | 13      |
| Phase 2 | 14      |
| Phase 3 | 11      |
| Phase 4 | 11      |
| Phase 5 | 13      |
| **Total** | **62**  |

---

## 12. Open Questions / Future Scope

| Area               | MVP Decision                  | Future Consideration                     |
|--------------------|-------------------------------|------------------------------------------|
| Mobile App         | Not in scope                  | React Native / Flutter                   |
| Real-time Tracking | HTTP polling (5s)             | WebSocket / SSE                          |
| Ratings & Reviews  | Not in scope                  | Dedicated microservice or folded into Order |
| Analytics          | Not in scope                  | Separate analytics pipeline (CDC → data warehouse) |
| CDN for images     | Base64 or local storage       | AWS S3 / CloudFront                      |
| Multi-language     | English only                  | i18n support                             |
| Dark stores        | Not in scope                  | Swiggy Instamart-style quick commerce    |
| Kubernetes         | Docker Compose                | Helm charts, K8s manifests               |

---

## 13. API Contracts (High-Level)

### User Service
```
POST   /users/register
POST   /users/login
GET    /users/{id}
PUT    /users/{id}
GET    /users/addresses
POST   /users/addresses
DELETE /users/addresses/{id}
POST   /users/subscriptions          # Subscribe to loyalty
GET    /users/{id}/subscription      # Active subscription status
```

### Restaurant Service
```
POST   /restaurants
GET    /restaurants/{id}
PUT    /restaurants/{id}
GET    /restaurants/{id}/menu
POST   /restaurants/{id}/menu/categories
POST   /restaurants/{id}/menu/items
PUT    /restaurants/{id}/menu/items/{itemId}
PATCH  /restaurants/{id}/menu/items/{itemId}/availability
```

### Cart Service
```
GET    /cart                           # Get current user's cart
POST   /cart/items                     # Add item to cart
PATCH  /cart/items/{itemId}            # Update quantity
DELETE /cart/items/{itemId}            # Remove item
POST   /cart/coupon                    # Apply coupon
DELETE /cart/coupon                    # Remove coupon
DELETE /cart                           # Clear cart
```

### Order Service
```
POST   /orders                         # Place order (from cart)
GET    /orders/{id}
GET    /orders                         # User's order history
PATCH  /orders/{id}/cancel
GET    /orders/{id}/status             # Current status
```

### Delivery Service
```
GET    /delivery/orders/{orderId}/tracking       # Real-time tracking
POST   /delivery/partners/{id}/location          # Partner location ping
PATCH  /delivery/assignments/{id}/status         # Update delivery status
```

### Payment Service
```
POST   /payments/initiate             # Create payment for order
POST   /payments/callback/razorpay    # Razorpay webhook
POST   /payments/callback/stripe      # Stripe webhook
GET    /payments/{id}                 # Payment status
POST   /payments/{id}/refund          # Initiate refund
```

### Notification Service
```
GET    /notifications                     # User's notifications
GET    /notifications/unread/count        # Unread badge count
PATCH  /notifications/{id}/read           # Mark as read
```

### Search/Catalog Service
```
GET    /search/restaurants?q=&cuisine=&city=&lat=&lng=&radius=
GET    /search/menu?restaurantId=&q=&vegetarian=
```

---

## 14. Non-Functional Requirements

| NFR                  | Target                                       |
|----------------------|----------------------------------------------|
| Response Time (API)  | < 200ms for 95th percentile (search), < 100ms (CRUD) |
| Availability         | 99.9% (target, MVP can be lower)             |
| Concurrent Users     | 100 simultaneous users (MVP)                 |
| Data Consistency     | Eventual consistency for async flows, strong consistency within service boundaries |
| Resilience           | Circuit breakers (Resilience4j), retry with backoff, graceful degradation |
| Security            | All endpoints behind Gateway with JWT validation, CORS configured, rate limiting |
