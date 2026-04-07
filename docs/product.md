# Product Vision: BookInfo Service Mesh Platform

## Problem Statement

Teams adopting Istio service mesh face two bad options for learning: Istio's own BookInfo sample (polyglot toy services with no real architecture) or enterprise platforms so complex that the mesh patterns are buried under layers of abstraction. There is nothing in between -- no **production-grade, opinionated reference** that pairs a properly built Kotlin + Spring Boot application with a real Istio deployment, showing how application-level concerns (resilience, security, observability) and mesh-level concerns (mTLS, traffic shaping, policy) work together in practice.

Developers need a project they can clone, study, and steal patterns from -- one where the services are worth studying on their own AND the Istio configuration is production-realistic, not a minimal demo.

## Vision

BookInfo is a **production-grade Kotlin + Spring Boot microservices platform that teaches modern Kubernetes deployment patterns with Istio service mesh.** Fifteen services model a realistic online bookstore -- identity, catalog, web and mobile BFFs, social proof, commerce, inventory, discovery, messaging, operational status, and two Vue.js + TypeScript frontends (customer-facing storefront and admin dashboard) -- deployed on Istio with mTLS, traffic management, distributed tracing, and CI/CD. A developer should be able to clone the repo, run three commands, and have the full platform running locally on patterns they can lift directly into their own projects.

## Goals

- Uses and showcases how to use Istio service mesh in a real Kotlin + Spring Boot microservices architecture
- Demonstrates production-grade application patterns (Resilience4j, Spring Security, OpenTelemetry) alongside mesh-level patterns (mTLS, traffic splitting, fault injection, authorization policies)
- Shows how application-level resilience and mesh-level resilience coexist and complement each other

## Target Users

- **Backend developers** learning Kotlin + Spring Boot in a microservices context
- **Platform engineers** adopting Istio and looking for production-grade deployment patterns
- **DevOps engineers** looking for Helm charts with Istio, proper networking, TLS, and CI/CD
- **Students and educators** who need a realistic (not trivial) service mesh example with real application code

## Design Principles

1. **Production-first** -- Every pattern in this repo should be safe to copy into a production codebase. No shortcuts that "work for demos" but fail under load.
2. **Cloud-agnostic** -- Runs on any conformant Kubernetes cluster with Istio. Helm values overlays for local (kind/minikube), GKE, EKS, and IBM Cloud, but zero vendor lock-in in the core.
3. **Secure by default** -- Istio mTLS for service-to-service encryption, Istio AuthorizationPolicy for mesh-level access control, Spring Security for application-level authorization, cert-manager for ingress TLS, no secrets in Git.
4. **Observable from day one** -- Structured JSON logs, OpenTelemetry traces integrated with Istio's tracing, Prometheus metrics via Micrometer + Istio telemetry, Kiali for mesh visualization, health endpoints that verify downstream dependencies.
5. **Fifteen services, phased delivery** -- The platform ships thirteen Kotlin backends plus two Vue.js frontends spanning identity, catalog, web/mobile BFFs, commerce, discovery, messaging, operations, and UI. Implementation is phased by dependency order, but every service is an equal part of the architecture.
6. **Backend and frontend, done well** -- Kotlin + Spring Boot for all backend services; Vue.js + TypeScript for both frontends. The backend focus is on doing Kotlin microservices right; the frontends demonstrate how modern SPAs consume the web BFF behind an Istio mesh, while mobile clients consume a mobile-specific BFF.
7. **Infrastructure as showcase** -- Every platform pattern (mesh, monitoring stack, policy engine) is included to demonstrate production Istio usage, not hidden or deferred.

## Service Architecture

Fifteen services organized by domain: thirteen Kotlin + Spring Boot backends and two Vue.js + TypeScript frontends. Backend services own their data, expose REST APIs, and participate in the Istio mesh with mTLS, AuthorizationPolicy, and telemetry. The `web-bff` and `mobile-bff` services provide client-specific API surfaces for web and mobile platforms, while both frontends are separately deployed services behind the same mesh. The target data model is **database-per-service**; early milestones may share a MySQL instance for local convenience while maintaining strict repository boundaries in code.

### storefront

Vue.js + TypeScript SPA for customers. The public-facing bookstore UI. Demonstrates:

- Modern SPA consuming the `web-bff` JSON API over the Istio mesh
- Vite build tooling, TypeScript strict mode, component-based architecture
- Nginx-served static assets in a minimal Docker image
- Istio VirtualService routing for the storefront alongside backend API routes
- Pages: book catalog, product detail (reviews, ratings, recommendations), search, cart, checkout, login

### admin

Vue.js + TypeScript SPA for operators and administrators. Internal dashboard for managing the bookstore. Demonstrates:

- Separate SPA consuming the `web-bff` API with admin-scoped endpoints
- Role-based UI: inventory management, review moderation, order management, service health overview
- Same tech stack as `storefront` (Vue.js, Vite, TypeScript, Nginx) but independently deployed
- Istio VirtualService routing on a separate path or hostname from `storefront`
- Demonstrates how two web frontends share a web BFF with role-scoped access

### productpage (Product Aggregation)

BookInfo-compatible product/catalog aggregation surface. It fans out to catalog, social-proof, commerce, and discovery services and returns product-oriented JSON that can be used directly in teaching scenarios or reused by BFFs where appropriate. Demonstrates:

- Downstream HTTP calls via WebClient with timeout configuration
- Resilience4j circuit breakers per downstream -- graceful degradation when any backend is unavailable
- Distributed tracing context propagation (automatic via Micrometer / OpenTelemetry, enhanced by Istio sidecar)
- Product detail aggregation without server-side rendering
- Backward-compatible BookInfo product flows while the platform BFFs own client-specific APIs

### web-bff

BFF for web clients, including `storefront` and `admin`. It exposes `/api/v1/web` and shapes responses for browser-based user experiences. Demonstrates:

- Role-scoped web API responses for customer and operator flows
- Downstream aggregation across `productpage`, catalog, social-proof, commerce, and discovery services
- Resilience4j circuit breakers, retries, and explicit timeouts per downstream
- OpenAPI documentation, Actuator health, and `ProblemDetail` error responses

### mobile-bff

BFF for mobile clients. It exposes `/api/v1/mobile` and shapes responses for mobile application constraints such as smaller payloads and mobile-specific API versioning. Demonstrates:

- Mobile-specific aggregation and response shaping
- Downstream calls with explicit timeouts and Resilience4j protection
- OpenAPI documentation, Actuator health, and `ProblemDetail` error responses
- Istio routing and authorization for a non-browser client surface

### users

Identity and access. User accounts, authentication, and session or token management. Demonstrates:

- Spring Security with JWT or OAuth2 resource-server patterns
- Istio `RequestAuthentication` for mesh-level JWT validation and identity propagation
- Per-service Flyway-managed schema (database-per-service boundary)
- Istio `AuthorizationPolicy` controlling which services may call `users`

### details

Book metadata. Read endpoint for catalog information. Demonstrates:

- Spring Data JPA with HikariCP connection pooling
- Spring Boot Actuator health checks with database health indicator
- Flyway schema migration on startup

### reviews

Written reviews with CRUD operations. Demonstrates:

- Input validation with Bean Validation (`@Valid`, `@Min`, `@Max`, `@Size`)
- Parameterized queries (JPA) preventing SQL injection
- Authorization enforcement on destructive operations (Spring Security + Istio AuthorizationPolicy)
- Inter-service call to `ratings` with tracing header propagation
- Pagination for unbounded result sets

### ratings

Numeric star ratings, read-only. Demonstrates:

- Lightweight query service with clean separation from `reviews`
- Request-scoped data access (no shared mutable state)

### orders

Order lifecycle -- cart, checkout, fulfillment status. Demonstrates:

- Saga or outbox pattern for multi-service coordination with `inventory` and `details`
- Idempotent create operations
- Istio traffic mirroring and canary routing for testing new order flows
- Resilience4j circuit breakers and timeouts on downstream calls

### inventory

Stock levels, availability, and reservation. Demonstrates:

- Write-heavy service with concurrency under contention
- Istio `DestinationRule` tuning and mesh-level rate limiting for hot paths
- Domain event publication when messaging infrastructure is present

### notifications

Outbound messaging triggered by domain events (order confirmations, review responses). Demonstrates:

- Async consumer path via Kafka or RabbitMQ
- Istio egress control (`ServiceEntry`) for external SMTP, push, or third-party APIs
- Clean separation between event consumption and delivery

### search

Full-text and faceted book search. Demonstrates:

- Search API backed by OpenSearch, Elasticsearch, or an agreed embedded index
- Istio connection-pool/routing defaults toward the search backend
- Graceful degradation in `productpage`, `web-bff`, or `mobile-bff` when search is unavailable

### recommendations

"Readers also liked" and related discovery. Demonstrates:

- Read-heavy service with clear API contracts to `productpage`, `web-bff`, and `mobile-bff`
- Fault injection and latency testing for aggregator resilience
- Cache strategy for recommendation results

### statuspage

Standalone operational status dashboard. Aggregates health from all services and presents current and historical uptime. Demonstrates:

- Fan-out health checks to all backend services via their Actuator endpoints
- Persistent incident and uptime history (own database)
- Public-facing status view independent of the main application frontends
- Istio AuthorizationPolicy allowing statuspage to reach all services' health endpoints
- Production pattern: dedicated status service that remains available even when the main application is degraded

## Platform capabilities

| Domain | Services | What the platform delivers |
| --- | --- | --- |
| **Identity** | `users` | Accounts, JWT/OAuth2, mesh-level identity via Istio `RequestAuthentication` |
| **Catalog** | `details`, `productpage` | Book metadata and product detail aggregation with resilience |
| **Web BFF** | `web-bff` | Browser-oriented API for `storefront` and `admin`, role-scoped aggregation, resilience |
| **Mobile BFF** | `mobile-bff` | Mobile-oriented API surface, response shaping, resilience |
| **Social proof** | `reviews`, `ratings` | Star ratings, written reviews with validation, pagination, safe persistence |
| **Commerce** | `orders`, `inventory` | Order lifecycle, stock management, saga coordination, idempotent writes |
| **Discovery** | `search`, `recommendations` | Full-text search, personalized suggestions, cache strategies |
| **Messaging** | `notifications` | Event-driven outbound messaging, Istio egress to external providers |
| **Operations** | `statuspage` | Aggregated health dashboard, incident history, uptime monitoring |
| **Customer UI** | `storefront` | Vue.js + TypeScript SPA for customers, Nginx-served, consuming `web-bff` API |
| **Admin UI** | `admin` | Vue.js + TypeScript SPA for operators, Nginx-served, consuming `web-bff` admin API |
| **Deployment** | all services | Helm charts, Kubernetes workloads, Istio mTLS, traffic management, authorization policies |
| **Observability** | all services | Micrometer metrics, OpenTelemetry traces, Istio telemetry, Kiali, Jaeger, Prometheus, Grafana |

## Platform Patterns Demonstrated

### Application-Level Patterns

| Pattern | Technology | What It Shows |
| --- | --- | --- |
| Circuit breaking | Resilience4j CircuitBreaker | Graceful degradation when downstream services are unhealthy |
| Retries & timeouts | Resilience4j Retry + WebClient timeouts | Controlled retry with backoff; fail fast on unresponsive services |
| Endpoint authorization | Spring Security | Role-based access on destructive endpoints (e.g., DELETE reviews) |
| Identity & authentication | Spring Security + JWT/OAuth2 | Token-based identity across services |
| Saga coordination | Outbox or choreography | Multi-service transaction consistency (orders + inventory) |
| Observability | Micrometer + OpenTelemetry + Actuator | Application-level metrics, distributed traces, structured logs, health checks |

### Mesh-Level Patterns (Istio)

| Pattern | Istio Resource | What It Shows |
| --- | --- | --- |
| Mutual TLS | PeerAuthentication (STRICT) | Automatic encryption of all service-to-service traffic |
| Traffic splitting | VirtualService + DestinationRule | Canary deployments, A/B testing, weighted routing between versions |
| Fault injection | VirtualService (fault) | Testing resilience by injecting delays and HTTP errors at the mesh level |
| Mesh authorization | AuthorizationPolicy | Mesh-enforced access control (which services can call which) |
| Request authentication | RequestAuthentication | JWT validation at the mesh level, identity propagation from `users` |
| Traffic mirroring | VirtualService (mirror) | Shadow traffic to new versions for testing without impacting users |
| Ingress routing | Istio Gateway + VirtualService | HTTPS entry point, path-based routing with TLS termination |
| Egress control | ServiceEntry | Controlled access to external services (SMTP, search clusters, APIs) |
| Observability | Kiali, Jaeger, Prometheus (Istio telemetry) | Mesh-wide visibility, service graph, trace correlation |
| Rate limiting | EnvoyFilter or Istio rate-limit | Protecting services from traffic spikes at the mesh level |

### Infrastructure Patterns

| Pattern | Technology | What It Shows |
| --- | --- | --- |
| TLS termination | cert-manager + Istio Gateway TLS | Automated certificate provisioning and renewal at the ingress |
| Network segmentation | Kubernetes NetworkPolicy | Defense-in-depth alongside Istio AuthorizationPolicy |
| Secret management | Kubernetes Secrets + ExternalSecret (optional) | No plaintext credentials in Git |
| Packaging | Helm 3 charts | Reproducible deployments with environment-specific overlays |
| CI/CD | GitHub Actions | Automated lint, test, build, scan, deploy pipeline |

## Non-Goals

- **The frontends are functional, not design showcases.** Both Vue.js SPAs provide working UIs but the primary teaching focus is backend services and mesh deployment patterns.
- **This is not a Kubernetes tutorial.** Basic Kubernetes knowledge is assumed.
- **This is not a Spring Boot tutorial.** The services follow standard Spring Boot conventions without excessive comments explaining Spring itself.
- **This is not an Istio-only project.** The application is independently well-architected. Istio augments it; it does not replace application-level patterns.

## Success Criteria

1. `kind create cluster && istioctl install && helm install bookinfo helm/bookinfo` gets a fully working platform in under 10 minutes
2. GitHub Actions CI passes on every PR (lint, test, build, scan, helm lint)
3. A developer can read the Helm templates and Istio resources and understand what each does
4. No CVEs in container images (Trivy scan clean)
5. Every service handles the "downstream is down" case gracefully (Resilience4j + Istio working together)
6. Istio mesh patterns (traffic splitting, fault injection, mTLS) are demonstrable with provided example configs
7. A new contributor can understand the architecture and make a meaningful PR within a day
8. The platform delivers all fifteen services (`storefront`, `admin`, `productpage`, `web-bff`, `mobile-bff`, `details`, `reviews`, `ratings`, `users`, `orders`, `inventory`, `notifications`, `search`, `recommendations`, `statuspage`) with per-service persistence where needed and mesh policy

## Technical Constraints

Authoritative version and stack details live in [tech.md](tech.md) and in [Agents.md](../Agents.md). At a high level: Kotlin on Java 21, Spring Boot, Gradle with a version catalog, MySQL with Flyway, Istio for the mesh, Helm for packaging, GitHub Actions for automation, Apache 2.0 license. Frontends: Vue.js 3 + TypeScript + Vite, served by Nginx.

## Milestones

Services are implemented in dependency order: catalog services first (others depend on them), then identity, then product aggregation and platform BFFs, then commerce and discovery, then messaging and operations. Frontends are built after the `web-bff` API stabilizes.

| Milestone | Deliverable | Depends On |
| --- | --- | --- |
| M1: Foundation | Gradle multi-module project builds, version catalog, empty Spring Boot apps for all backend services | -- |
| M2: Catalog services | `details`, `ratings`, `reviews` implemented with JPA, Flyway, validation, tests | M1 |
| M3: Identity & aggregation | `users` (JWT/OAuth2, Spring Security), `productpage` (product aggregation, Resilience4j), `web-bff`, and `mobile-bff` implemented and tested | M1, M2 |
| M4: Commerce & discovery | `orders`, `inventory`, `search`, `recommendations` implemented with per-service data stores and tests | M3 |
| M5: Messaging & operations | `notifications` (event consumer, egress) and `statuspage` (aggregated health, incident history) implemented | M4 |
| M6: Frontends | `storefront` (customer SPA) and `admin` (operator SPA), both Vue.js + TypeScript consuming `web-bff` API; Vite build, Nginx Docker images | M3 |
| M7: Containers | Multi-stage Dockerfiles for all services, images build and run locally with docker-compose | M2, M6 |
| M8: Kubernetes base | Helm chart deploys to kind, Deployments + Services + NetworkPolicy for all fifteen services | M7 |
| M9: Istio mesh | Istio installed, PeerAuthentication (mTLS), AuthorizationPolicy, VirtualService routing, DestinationRules for all services | M8 |
| M10: Istio advanced | Traffic splitting, fault injection, traffic mirroring, egress control, rate limiting, RequestAuthentication | M9 |
| M11: Observability | Kiali, Jaeger, Prometheus/Grafana dashboards configured with Istio telemetry + app-level metrics | M9 |
| M12: Pipeline | GitHub Actions CI green, deploy workflow functional | M7, M8 |
| M13: Polish | README, QUICKSTART, Trivy scan clean, example Istio scenarios documented | M10, M11, M12 |
