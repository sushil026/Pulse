# Pulse

**Real-time notification workflow engine — SaaS platform**

Pulse lets product teams define, deliver, and observe notifications across any channel without rebuilding the plumbing every time. You model a notification as a workflow (stateful, scheduled, broadcast, etc.), call the API once, and Pulse handles routing, persistence, retries, and real-time push to your end users.

---

## What is Pulse?

Pulse is a multi-tenant SaaS platform that sits between your backend services and your users. It exposes a simple HTTP/WebSocket API so any service can fire a notification in a single call, while Pulse internally runs a workflow engine that:

- persists notification state with full audit history
- routes to one or more delivery channels (WebSocket, push, email, SMS, webhook)
- re-delivers on failure with configurable retry policies
- broadcasts to arbitrarily large subscriber sets without caller involvement
- executes scheduled and recurring notification jobs

Tenants onboard via the dashboard, define their notification schemas and delivery rules, embed the JS/mobile SDK (or call the REST API directly), and Pulse does the rest.

---

## Notification Types

| Type | Description |
|------|-------------|
| **Stateful** | Notifications that move through a defined lifecycle (e.g. `created → delivered → read → archived`). State transitions are persisted and queryable. Ideal for order updates, support tickets, or any event the user needs to act on. |
| **Fire and Forget** | One-shot notifications with no state tracking after delivery. Low overhead. Ideal for transient alerts, log tails, or any event where only the latest value matters. |
| **Append** | An ordered, append-only stream of notifications attached to a resource (e.g. activity feed, audit log, comment thread). Consumers can paginate history or subscribe to the tail in real time. |
| **Scheduled** | Notifications triggered at a future timestamp or on a cron expression. The workflow engine persists the schedule, handles clock skew, and fires delivery at the right moment even across restarts. |
| **Broadcast** | Fan-out notifications sent to all subscribers matching a topic, segment, or tag — potentially millions of recipients. Pulse shards the fan-out internally so the caller makes one API call regardless of audience size. |

---

## Monorepo Structure

```
pulse/
├── apps/
│   ├── dashboard/          # Tenant SaaS dashboard (Next.js, port 3000)
│   ├── demo-rapido/        # Demo: ride-hailing notification flows (Next.js, port 3001)
│   └── demo-amazon/        # Demo: e-commerce order notification flows (Next.js)
│
├── packages/
│   ├── ui/                 # Shared React component library (@repo/ui)
│   ├── eslint-config/      # Shared ESLint configs (@repo/eslint-config)
│   └── typescript-config/  # Shared tsconfig presets (@repo/typescript-config)
│
├── services/               # Backend microservices (Java / Spring Boot)
│   ├── pulse-api/          # Public-facing REST + WebSocket API gateway
│   ├── workflow-engine/    # Notification workflow orchestration
│   ├── delivery-service/   # Multi-channel delivery (WebSocket, push, email, SMS)
│   ├── relay-service/      # Broadcast fan-out and topic relay
│   └── websocket-gateway/  # Persistent WebSocket connection server
│
├── infrastructure/
│   ├── docker/             # Docker Compose for local development
│   ├── kafka/              # Kafka topic definitions and configs
│   └── postgres/           # Database migrations and schema
│
├── sdk/
│   └── java/               # Java client SDK for Pulse API
│
├── docs/                   # Architecture docs and API references
├── turbo.json              # Turborepo pipeline config
└── package.json            # Root workspace manifest
```

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend apps | Next.js 16, React 19, TypeScript |
| Backend services | Java 21, Spring Boot 3, Spring WebFlux |
| Message broker | Apache Kafka |
| Primary database | PostgreSQL 16 |
| Cache / presence | Redis 7 |
| Real-time push | WebSocket (STOMP over WS) |
| Monorepo tooling | Turborepo, npm workspaces |
| Containerisation | Docker, Docker Compose |

---

## Quick Start

> Prerequisites: Node >= 18, Java 21, Docker

```bash
# 1. Clone and install JS dependencies
git clone https://github.com/sushil026/pulse.git
cd pulse
npm install

# 2. Start backing services (Kafka, PostgreSQL, Redis)
docker compose -f infrastructure/docker/docker-compose.yml up -d

# 3. Run database migrations
# (migration tooling TBD — see infrastructure/postgres/)

# 4. Start backend services
# (service startup scripts TBD — see services/)

# 5. Start the dashboard
npm run dev --filter=dashboard
# Open http://localhost:3000
```

> Full local setup guide coming soon in `docs/`.

---

## Contributing

See `AGENTS.md` for architectural context, coding conventions, and build order guidance intended for both human developers and AI coding agents.
