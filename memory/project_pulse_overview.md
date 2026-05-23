---
name: project-pulse-overview
description: Pulse is a multi-tenant real-time notification workflow engine SaaS. Monorepo with Next.js apps, Java/Spring Boot services, Kafka, PostgreSQL, Redis.
metadata:
  type: project
---

Pulse is a SaaS notification workflow engine. Tenants call one API to trigger any notification type; Pulse handles routing, persistence, delivery, retries, and real-time push.

**Why:** Founders want to build this as a product — not internal tooling.

**5 notification types:** Stateful, Fire and Forget, Append, Scheduled, Broadcast.

**Stack:**
- Frontend: Next.js 16 / React 19 / TypeScript (Turborepo monorepo, npm workspaces)
- Backend: Java 21 / Spring Boot 3 (5 microservices, all empty scaffolds as of 2026-05-23)
- Broker: Kafka
- DB: PostgreSQL 16 (outbox pattern, RLS for multi-tenancy)
- Cache/presence: Redis 7
- Real-time: WebSocket (STOMP)

**Services (all empty scaffolds):** pulse-api (8080), workflow-engine (8081), delivery-service (8082), relay-service (8083), websocket-gateway (8084).

**Apps:** dashboard (port 3000), demo-rapido (port 3001), demo-amazon (scaffolded empty).

**How to apply:** Refer to AGENTS.md at repo root for full architectural context before making any changes.
