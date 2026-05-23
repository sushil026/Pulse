# AGENTS.md — Pulse Project Context

This file is the authoritative context document for AI coding agents (Claude Code, Cursor, Codex, etc.) working in this repository. Read this before making any changes.

---

## Project Overview

Pulse is a **multi-tenant, real-time notification workflow engine** delivered as a SaaS platform. It abstracts the full lifecycle of a notification — creation, routing, persistence, delivery, retries, state transitions, and real-time push — behind a simple API so product teams never have to build this infrastructure themselves.

**Key value propositions:**
- One API call to trigger any notification type, regardless of delivery complexity
- Full audit trail and state history for every notification
- Real-time delivery via persistent WebSocket connections
- Tenant isolation: each tenant has its own schemas, delivery rules, and observability
- Scalable broadcast fan-out decoupled from the caller

---

## Repository Layout

```
pulse/
├── apps/
│   ├── dashboard/          # Tenant SaaS dashboard
│   ├── demo-rapido/        # Demo app: ride-hailing notifications
│   └── demo-amazon/        # Demo app: e-commerce order notifications
│
├── packages/
│   ├── ui/                 # Shared React component library
│   ├── eslint-config/      # Shared ESLint configurations
│   └── typescript-config/  # Shared TypeScript tsconfig presets
│
├── services/
│   ├── pulse-api/          # API gateway service
│   ├── workflow-engine/    # Workflow orchestration service
│   ├── delivery-service/   # Multi-channel delivery service
│   ├── relay-service/      # Broadcast / fan-out relay service
│   └── websocket-gateway/  # WebSocket connection server
│
├── infrastructure/
│   ├── docker/             # Docker Compose for local dev
│   ├── kafka/              # Kafka topic configs and ACLs
│   └── postgres/           # Flyway/Liquibase migrations
│
├── sdk/
│   └── java/               # Java client SDK
│
└── docs/                   # Architecture decision records, API specs
```

---

## Apps

### `apps/dashboard`
- **Tech:** Next.js 16, React 19, TypeScript
- **Port:** 3000
- **Purpose:** The main tenant-facing SaaS dashboard. Tenants log in here to manage API keys, define notification schemas, configure delivery channels, set up routing rules, and observe notification analytics and audit logs.
- **Depends on:** `@repo/ui`, `@repo/eslint-config`, `@repo/typescript-config`

### `apps/demo-rapido`
- **Tech:** Next.js 16, React 19, TypeScript
- **Port:** 3001
- **Purpose:** A demo application simulating a ride-hailing platform (Rapido-like). Demonstrates Pulse notification flows for ride lifecycle events: booking confirmed, driver assigned, driver arriving, ride started, ride completed, payment processed.
- **Depends on:** `@repo/ui`, `@repo/eslint-config`, `@repo/typescript-config`

### `apps/demo-amazon`
- **Tech:** Next.js 16, React 19, TypeScript (scaffolded, not yet populated)
- **Purpose:** A demo application simulating an e-commerce platform (Amazon-like). Will demonstrate Pulse notification flows for order lifecycle: order placed, payment confirmed, packed, shipped, out for delivery, delivered, return initiated.

---

## Packages

### `packages/ui` (`@repo/ui`)
- Shared React component library used across all `apps/`
- Components: `Button`, `Card`, `Code` (initial set; grows as dashboard is built)
- Do not add business logic here — pure presentational components only

### `packages/eslint-config` (`@repo/eslint-config`)
- Exports: `base`, `next`, `react-internal`
- All apps and packages extend one of these configs

### `packages/typescript-config` (`@repo/typescript-config`)
- Exports: `base.json`, `nextjs.json`, `react-library.json`
- All packages reference these via `extends`

---

## Services

All backend services are **Java 21 / Spring Boot 3** microservices. They communicate with each other via Kafka (async) and REST (sync, internal only). All services are currently scaffolded as empty directories.

### `services/pulse-api`
- **Role:** Public-facing API gateway — the single entry point for all tenant and end-user traffic
- **Responsibilities:**
  - Authenticate and authorize API requests (API key validation, JWT for dashboard)
  - Validate incoming notification payloads against tenant-defined schemas
  - Write notification records to PostgreSQL via the outbox pattern
  - Publish outbox events to Kafka for downstream processing
  - Expose REST endpoints for notification CRUD, tenant management, analytics
  - Expose WebSocket endpoint for real-time subscription (proxied to websocket-gateway)
- **Listens on:** `8080`

### `services/workflow-engine`
- **Role:** Orchestrates the lifecycle of every notification
- **Responsibilities:**
  - Consume `notification.created` events from Kafka
  - Apply workflow rules: routing logic, retry policies, state machine transitions
  - Publish `notification.route` commands to delivery-service and relay-service
  - Persist state transitions to PostgreSQL
  - Manage scheduled notifications: poll the schedule table and fire delivery at the right time
  - Handle dead-letter events and escalation rules

### `services/delivery-service`
- **Role:** Executes actual delivery to end users across channels
- **Responsibilities:**
  - Consume `notification.deliver` commands from Kafka
  - Route to the correct channel handler: WebSocket push, FCM/APNs push, email (SES/SendGrid), SMS (Twilio), outbound webhook
  - Record delivery receipts (sent, delivered, failed) back to PostgreSQL
  - Retry failed deliveries with exponential backoff up to the configured limit
  - Publish `notification.delivered` / `notification.failed` events

### `services/relay-service`
- **Role:** Handles broadcast fan-out at scale
- **Responsibilities:**
  - Consume `notification.broadcast` commands from Kafka
  - Resolve the subscriber list for a topic/segment from PostgreSQL (paginated in batches)
  - Re-publish per-recipient `notification.deliver` commands to Kafka
  - Track broadcast progress and completion metrics
  - Shard large broadcasts across multiple consumer instances

### `services/websocket-gateway`
- **Role:** Maintains persistent WebSocket connections to end-user clients
- **Responsibilities:**
  - Accept and manage WebSocket connections (STOMP or raw WS)
  - Track active connection presence in Redis (connection ID → tenant + user mapping)
  - Consume `ws.push` commands from Kafka and push to the correct connection
  - Publish `user.connected` / `user.disconnected` events so other services know presence
  - Handle reconnection and message replay for briefly-offline clients

---

## Data Stores

### PostgreSQL (primary database)

Schema overview — all tables are tenant-scoped via `tenant_id`:

| Table | Description |
|-------|-------------|
| `tenants` | Tenant accounts, API keys, plan/quota metadata |
| `notification_schemas` | Tenant-defined notification type schemas (JSON Schema) |
| `notifications` | Every notification record with current state, type, payload, timestamps |
| `notification_events` | Append-only state transition log (audit trail) for each notification |
| `delivery_attempts` | Each delivery attempt per channel with status and error detail |
| `subscriptions` | User↔topic subscription mappings for broadcast routing |
| `schedules` | Pending scheduled notifications with `fire_at` timestamp and cron expression |
| `outbox` | Transactional outbox table — events written atomically with the notification row, consumed by Kafka publisher |

**Key constraint:** All writes that must be atomic with a Kafka publish go through the outbox table. No service publishes to Kafka directly from a business transaction.

### Redis

| Usage | Key Pattern | TTL |
|-------|-------------|-----|
| WebSocket presence | `ws:conn:{tenantId}:{userId}` → connection metadata | Session-scoped (evicted on disconnect) |
| Connection index | `ws:tenant:{tenantId}:connections` → set of connection IDs | Session-scoped |
| API key cache | `apikey:{hashedKey}` → tenant + permissions JSON | 5 minutes |
| Rate limiting | `ratelimit:{tenantId}:{window}` → request count | 1 minute sliding window |
| Notification state cache | `notif:{notificationId}:state` → current state | 10 minutes (write-through) |
| Idempotency keys | `idempotent:{tenantId}:{key}` → notification ID | 24 hours |

### Kafka Topics

| Topic | Producer | Consumer(s) | Description |
|-------|----------|-------------|-------------|
| `notification.created` | pulse-api (outbox relay) | workflow-engine | New notification ingested |
| `notification.route` | workflow-engine | delivery-service, relay-service | Workflow has resolved routing |
| `notification.deliver` | workflow-engine, relay-service | delivery-service | Deliver to a specific recipient+channel |
| `notification.broadcast` | workflow-engine | relay-service | Initiate a broadcast fan-out |
| `notification.state` | workflow-engine, delivery-service | pulse-api | State transition events for audit log |
| `notification.delivered` | delivery-service | workflow-engine | Successful delivery receipt |
| `notification.failed` | delivery-service | workflow-engine | Failed delivery — may trigger retry or escalation |
| `ws.push` | delivery-service | websocket-gateway | Push a message to a specific WebSocket connection |
| `user.presence` | websocket-gateway | workflow-engine, relay-service | User connected / disconnected events |
| `schedule.tick` | workflow-engine (scheduler) | workflow-engine | Internal clock tick for scheduled notification evaluation |

**Partition strategy:** All topics are partitioned by `tenantId` to preserve per-tenant ordering. High-volume broadcast topics (`notification.deliver`) use a higher partition count and are partitioned by `recipientId` to parallelize delivery.

---

## Notification Types

### Stateful
A notification that moves through a defined state machine. The workflow engine enforces valid transitions and persists each transition to `notification_events`. The caller and the end user can query the current state at any time.

Example states: `CREATED → ROUTING → DELIVERED → READ → ARCHIVED`

Use cases: order status updates, support ticket lifecycle, document approval flows.

### Fire and Forget
A notification with no state tracking after the initial delivery attempt. The workflow engine routes it to delivery-service once; no retry unless the tenant has configured a retry policy at the schema level. Delivery receipts are still recorded but there is no queryable lifecycle.

Use cases: transient alerts, real-time price ticks, ephemeral presence events.

### Append
An ordered stream of notifications attached to a resource identifier (e.g. `orderId`, `chatRoomId`). New entries are always appended to the tail; the stream is never mutated. Clients can paginate history via REST or subscribe to the live tail via WebSocket.

Use cases: activity feeds, audit logs, comment threads, chat messages.

### Scheduled
A notification with a `fireAt` timestamp or `cronExpression`. The workflow engine persists the schedule in the `schedules` table and polls it. When the fire time arrives, it publishes to `notification.created` as if the notification had just been received. Supports one-shot and recurring schedules.

Use cases: reminder notifications, weekly digest emails, SLA deadline alerts.

### Broadcast
A notification sent to all subscribers of a topic or all users matching a segment filter. The caller makes one API call; relay-service resolves the subscriber list and fans out individual `notification.deliver` commands. The caller can poll broadcast progress via the notification ID.

Use cases: product announcements, promotional campaigns, system-wide alerts, feature release notifications.

---

## Key Architectural Decisions

### Outbox Pattern
`pulse-api` never publishes to Kafka directly inside a business transaction. Instead it writes an outbox record atomically with the notification row in PostgreSQL. A separate outbox relay process (Debezium CDC or a polling publisher) reads the outbox and publishes to Kafka. This guarantees no lost events even if Kafka is temporarily unavailable.

### WebSocket Gateway as a Dedicated Service
WebSocket connections are stateful and long-lived. Keeping them in a dedicated service (`websocket-gateway`) means:
- Other services remain stateless and horizontally scalable
- WebSocket-specific concerns (connection management, reconnection, presence) are isolated
- `delivery-service` pushes to WebSocket via Kafka, not direct TCP, so it never needs to know which server holds a connection

### Multi-Tenancy Model
All database tables carry a `tenant_id` column with a NOT NULL foreign key to `tenants`. Row-level security (RLS) is enforced in PostgreSQL for all tenant data tables. `pulse-api` sets the tenant context on every request after API key validation. No cross-tenant data access is possible at the query level.

### Schema-First Notifications
Tenants define notification schemas (JSON Schema) in the dashboard before sending notifications. `pulse-api` validates every incoming notification against the tenant's schema before accepting it. This prevents malformed data from entering the pipeline and enables the dashboard to render typed notification UIs.

### Delivery Channel Abstraction
`delivery-service` uses a pluggable channel handler interface. Each channel (WebSocket, FCM, email, SMS, webhook) is an independent implementation. Adding a new channel does not require changes to the workflow engine or routing logic — only a new handler registered in delivery-service.

### Idempotency
Every API call that creates a notification accepts an optional `Idempotency-Key` header. `pulse-api` checks Redis before processing; duplicate keys return the original notification response without re-processing. Keys expire after 24 hours.

---

## Build Order

When building out this project from scratch, follow this dependency order:

1. **Infrastructure** (`infrastructure/`)
   - Docker Compose with PostgreSQL, Redis, Kafka
   - Database migrations (tenants, notifications, outbox tables)
   - Kafka topic creation scripts

2. **`pulse-api`** (services)
   - Core REST API skeleton, tenant auth, notification schema CRUD
   - Outbox writer (no Kafka integration yet — just write to outbox)

3. **Outbox relay**
   - Debezium connector or polling publisher that reads `outbox` and publishes to Kafka

4. **`workflow-engine`** (services)
   - Consume `notification.created`, implement state machine, publish `notification.deliver`
   - Scheduled notification job

5. **`websocket-gateway`** (services)
   - WebSocket server, presence tracking in Redis, consume `ws.push` from Kafka

6. **`delivery-service`** (services)
   - WebSocket channel handler first (wires up with websocket-gateway)
   - Add additional channels incrementally

7. **`relay-service`** (services)
   - Broadcast fan-out, subscription management

8. **`apps/dashboard`** (frontend)
   - Tenant onboarding, API key management, notification schema editor
   - Real-time notification log viewer

9. **Demo apps** (`apps/demo-rapido`, `apps/demo-amazon`)
   - Built last as validation that the full stack works end-to-end

10. **`sdk/java`**
    - Java client SDK wrapping the pulse-api REST + WebSocket API

---

## Coding Conventions

### Java / Spring Boot (services)
- Java 21 — use records for DTOs, sealed interfaces for sum types, pattern matching where natural
- Spring WebFlux (reactive) preferred over Spring MVC for services that handle high fan-out (websocket-gateway, relay-service); Spring MVC acceptable for pulse-api and workflow-engine
- Package structure per service: `api` (controllers/handlers), `domain` (entities, value objects), `application` (use cases / services), `infrastructure` (Kafka, persistence, Redis adapters)
- Database access via Spring Data JPA or jOOQ — no raw JDBC except for bulk operations
- All Kafka producers and consumers defined in the `infrastructure` package
- Flyway for database migrations; migration files in `infrastructure/postgres/migrations/`
- No `@Autowired` field injection — constructor injection only
- DTOs must not leak domain objects across service boundaries

### TypeScript / Next.js (apps + packages)
- Strict TypeScript (`"strict": true`) — no `any`, no type assertions without a comment explaining why
- Next.js App Router (`app/` directory) — no `pages/` directory
- Server Components by default; add `"use client"` only when interactivity requires it
- Shared components go in `packages/ui` — app-specific components stay in the app
- No inline styles — use CSS Modules (`.module.css`) or Tailwind (if added)
- Fetch data in Server Components or Route Handlers; no client-side data fetching via `useEffect` unless real-time (WebSocket)

### General
- Do not commit secrets, `.env` files, or API keys
- All Kafka topic names and PostgreSQL table names are `snake_case`
- REST API paths are `kebab-case` (`/notification-schemas`, not `/notificationSchemas`)
- JSON field names in API payloads are `camelCase`
- Every service must have a `/health` endpoint returning `200 OK` when healthy
- Docker images must be built from the service directory; multi-stage builds to keep images small

---

## Environment Variables (reference)

| Variable | Used by | Description |
|----------|---------|-------------|
| `DATABASE_URL` | all services | PostgreSQL JDBC URL |
| `REDIS_URL` | pulse-api, websocket-gateway, delivery-service | Redis connection URL |
| `KAFKA_BOOTSTRAP_SERVERS` | all services | Kafka broker addresses |
| `PULSE_API_KEY_SECRET` | pulse-api | Secret for hashing API keys before storage |
| `JWT_SECRET` | pulse-api | Secret for signing dashboard JWTs |
| `NEXT_PUBLIC_API_URL` | dashboard, demo apps | Base URL of pulse-api |
| `NEXT_PUBLIC_WS_URL` | dashboard, demo apps | WebSocket URL for real-time subscriptions |

---

## Local Development URLs

| Service | URL |
|---------|-----|
| Dashboard | http://localhost:3000 |
| Demo Rapido | http://localhost:3001 |
| pulse-api | http://localhost:8080 |
| workflow-engine | http://localhost:8081 |
| delivery-service | http://localhost:8082 |
| relay-service | http://localhost:8083 |
| websocket-gateway | http://localhost:8084 |
| PostgreSQL | localhost:5432 |
| Redis | localhost:6379 |
| Kafka | localhost:9092 |
| Kafka UI | http://localhost:8090 |
