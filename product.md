# Product Vision: BookInfo Service Mesh Platform

## Problem Statement

Teams adopting Istio service mesh face a steep learning curve. The existing examples in the ecosystem are either trivially simple (single-container demos that show routing but skip real-world concerns) or overwhelmingly complex (full enterprise platforms that bury mesh concepts under layers of abstraction). There is nothing in between -- no **production-grade, opinionated reference architecture** that demonstrates how Istio patterns should be applied to real microservices with actual database access, inter-service communication, input validation, and proper observability.

Developers need a project they can clone, study, and steal patterns from. Not a toy. Not a monolith disguised as microservices. A properly structured, secure-by-default, observable platform that works on any Kubernetes cluster and shows the full mesh lifecycle -- from ingress to mTLS to circuit breaking to canary deploys.

## Vision

BookInfo is a **batteries-included reference platform** for teams building Kotlin microservices on Istio. A developer should be able to clone the repo, run three commands, and have a fully functional service mesh running locally with TLS, mTLS, circuit breaking, distributed tracing, and proper CI/CD -- all on patterns they can lift directly into their own projects.

## Target Users

- **Platform engineers** evaluating Istio for their organization
- **Backend developers** learning Kotlin + Spring Boot in a microservices context
- **DevOps engineers** looking for Helm/Kustomize patterns for service mesh deployments
- **Students and educators** who need a realistic (not trivial) microservices example

## Design Principles

1. **Production-first** -- Every pattern in this repo should be safe to copy into a production codebase. No shortcuts that "work for demos" but fail under load.
2. **Cloud-agnostic** -- Runs on any conformant Kubernetes cluster. Helm values overlays for local (kind/minikube), GKE, EKS, and IBM Cloud, but zero vendor lock-in in the core.
3. **Secure by default** -- mTLS between all services, TLS at the ingress, no secrets in Git, parameterized queries everywhere, authorization policies restricting service-to-service communication.
4. **Observable from day one** -- Structured JSON logs, OpenTelemetry traces, Prometheus metrics via Micrometer, health endpoints that actually verify downstream dependencies.
5. **Minimal but complete** -- Four services is enough to demonstrate mesh patterns (routing, circuit breaking, fault injection, traffic splitting). Not so many that the project becomes unwieldy.
6. **One language, done well** -- Kotlin + Spring Boot across all services. Polyglot is interesting in theory but adds cognitive overhead without teaching mesh concepts. The mesh is language-agnostic by nature; the services should be easy to read.

## Service Architecture

### productpage (Aggregator / BFF)

The front door. Aggregates data from details, reviews, and ratings into a single page. Demonstrates:

- Downstream HTTP calls via WebClient with timeout configuration
- Circuit breaker pattern (Resilience4j) -- graceful degradation when reviews or ratings are down
- Distributed tracing context propagation (automatic via Spring Cloud Sleuth / Micrometer)
- Server-side rendering with Thymeleaf (keeps the demo self-contained, no separate frontend build)

### details

The simplest service. Single read endpoint. Demonstrates:

- Spring Data JPA with HikariCP connection pooling
- Spring Boot Actuator health checks with database health indicator
- Flyway schema migration on startup

### reviews

The most complex service. Read, write, and delete operations. Demonstrates:

- Input validation with Bean Validation (`@Valid`, `@Min`, `@Max`, `@Size`)
- Parameterized queries (JPA) preventing SQL injection
- Authorization enforcement on destructive operations
- Inter-service call to ratings with tracing header propagation
- Pagination for unbounded result sets

### ratings

Read-only ratings data. Demonstrates:

- Lightweight query service
- Clean separation from the reviews service (ratings are a separate concern)
- Request-scoped data access (no shared mutable state)

### MySQL 8.4

Single shared database with Flyway-managed schema. In production, each service would own its own database (database-per-service pattern), but a shared database keeps the local setup simple while the code still uses proper repository boundaries.

## Istio Mesh Patterns Demonstrated


| Pattern                 | Istio Resource                         | What It Shows                                                     |
| ----------------------- | -------------------------------------- | ----------------------------------------------------------------- |
| Ingress routing         | Gateway + VirtualService               | HTTP/HTTPS entry point, path-based routing to services            |
| Mutual TLS              | PeerAuthentication + DestinationRule   | Encrypted service-to-service communication, zero-trust networking |
| Service authorization   | AuthorizationPolicy                    | Only productpage can call details; only reviews can call ratings  |
| External service access | ServiceEntry                           | Controlled egress to external MySQL (optional)                    |
| Traffic splitting       | VirtualService weight-based routing    | Canary deployments between v1/v2 of reviews                       |
| Circuit breaking        | DestinationRule outlier detection      | Eject unhealthy endpoints from the load balancing pool            |
| Fault injection         | VirtualService fault block             | Test resilience by injecting delays or HTTP errors                |
| Observability           | Envoy-native telemetry (Telemetry API) | Metrics, traces, and access logs without Mixer                    |


## Non-Goals

- **This is not a UI showcase.** The frontend is intentionally simple. The focus is on backend services and mesh infrastructure.
- **This is not a Kubernetes tutorial.** Basic Kubernetes knowledge is assumed. The README explains Istio concepts, not what a Pod is.
- **This is not a Spring Boot tutorial.** The services follow standard Spring Boot conventions without excessive comments explaining Spring itself.
- **No microservice framework lock-in.** The Istio patterns work identically regardless of what runs inside the containers. Swapping a service to Ktor or Go should require zero Istio config changes.

## Success Criteria

1. `kind create cluster && istioctl install && helm install bookinfo helm/bookinfo` gets a fully working mesh in under 5 minutes
2. GitHub Actions CI passes on every PR (lint, test, build, scan, helm lint)
3. A developer unfamiliar with Istio can read the Helm templates and understand what each mesh resource does
4. No CVEs in container images (Trivy scan clean)
5. Every service handles the "downstream is down" case gracefully
6. A new contributor can understand the architecture and make a meaningful PR within a day

## Technical Constraints

- **Kotlin 2.0+** with Spring Boot 3.3+ on Java 21
- **Gradle** with version catalog (not Maven)
- **MySQL 8.4** as the backing data store
- **Istio 1.20+** (current stable APIs, no Mixer, no alpha CRDs)
- **Helm 3** charts with Kustomize as an alternative
- **GitHub Actions** for CI/CD
- **Apache 2.0 license**

## Milestones


| Milestone      | Deliverable                                                                       | Depends On |
| -------------- | --------------------------------------------------------------------------------- | ---------- |
| M1: Foundation | Gradle multi-module project builds, version catalog, empty Spring Boot apps start | --         |
| M2: Services   | All 4 services implemented in Kotlin, passing unit tests, Flyway migrations       | M1         |
| M3: Containers | Multi-stage Dockerfiles, images build and run locally with docker-compose         | M2         |
| M4: Mesh       | Helm chart deploys to kind with Istio, all mesh patterns configured               | M2         |
| M5: Pipeline   | GitHub Actions CI green, deploy workflow functional                               | M3, M4     |
| M6: Polish     | README, QUICKSTART, Trivy scan clean                                              | M5         |


