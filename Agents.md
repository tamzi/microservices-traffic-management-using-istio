# BookInfo Microservices — Agent Rules

## Precedence

- This file is the project-specific source of truth for agent behavior in this repository.
- If a generic Kotlin/Spring convention conflicts with a BookInfo-specific rule here, this file wins.
- Dependencies listed in `gradle/libs.versions.toml` are approved options for the repo, not proof that every service already uses them.

## Project Structure

Gradle multi-module monorepo. Thirteen Kotlin + Spring Boot backend services and two Vue.js + TypeScript frontends under `services/`.

```
services/
├── storefront/        # Customer-facing Vue.js + TypeScript SPA (Vite, Nginx)
├── admin/             # Operator/admin Vue.js + TypeScript SPA (Vite, Nginx)
├── productpage/       # Product/catalog aggregation API retained for BookInfo-compatible flows
├── web-bff/           # BFF for web clients (storefront and admin)
├── mobile-bff/        # BFF for mobile clients
├── users/             # Identity — accounts, JWT/OAuth2
├── details/           # Catalog — book metadata
├── reviews/           # Social proof — reviews CRUD, calls ratings
├── ratings/           # Social proof — read-only star ratings
├── orders/            # Commerce — order lifecycle, saga coordination
├── inventory/         # Commerce — stock levels, availability
├── notifications/     # Messaging — event-driven outbound messages
├── search/            # Discovery — full-text and faceted search
├── recommendations/   # Discovery — "readers also liked"
└── statuspage/        # Operations — aggregated health and uptime
```

Group: `org.bookinfo`

Package pattern: `org.bookinfo.<service>`; hyphenated module names use compact package names such as `org.bookinfo.webbff` and `org.bookinfo.mobilebff`.

All services listen on port `9080`.

## Stack

| Layer | Technology |
|-------|-----------|
| Language (backend) | Kotlin 2.3.20 |
| Language (frontend) | TypeScript, Vue.js 3 |
| Framework (backend) | Spring Boot 4.0.5, Spring Cloud 2025.1 |
| Framework (frontend) | Vue.js 3 + Vite, served by Nginx |
| Build | Gradle 9.4.1 (Kotlin DSL), version catalog at `gradle/libs.versions.toml` |
| Database | MySQL 8.4, Flyway migrations |
| Testing | JUnit 6.0.3, MockK, Testcontainers 2.0.4, AssertJ |
| Observability | Micrometer, OpenTelemetry, Spring Boot Actuator |
| Resilience | Resilience4j circuit breaker + retry on all downstream calls |
| Service Mesh | Istio (PeerAuthentication, VirtualService, DestinationRule, AuthorizationPolicy, Gateway) |
| Security | Istio mTLS (STRICT), Istio AuthorizationPolicy, Kubernetes NetworkPolicy, cert-manager for TLS, Spring Security for authz |
| Static Analysis | Detekt 1.23.8, JaCoCo |
| Ingress | Istio Gateway + VirtualService for path-based routing with TLS termination |
| Mesh Observability | Kiali, Jaeger, Prometheus + Grafana (Istio telemetry) |
| Packaging | Helm 3 charts, multi-stage Docker (Eclipse Temurin 21) |
| CI/CD | GitHub Actions |

## Services

- `storefront` — Vue.js + TypeScript SPA for customers; Vite build, Nginx Docker image, no Spring Boot
- `admin` — Vue.js + TypeScript SPA for operators; Vite build, Nginx Docker image, no Spring Boot
- `productpage` — package `org.bookinfo.productpage`; product/catalog aggregation API retained for BookInfo-compatible flows
- `web-bff` — package `org.bookinfo.webbff`; BFF for web clients, including `storefront` and `admin`
- `mobile-bff` — package `org.bookinfo.mobilebff`; BFF for mobile clients
- `users` — package `org.bookinfo.users`; identity and authentication
- `details` — package `org.bookinfo.details`; book metadata
- `reviews` — package `org.bookinfo.reviews`; reviews CRUD
- `ratings` — package `org.bookinfo.ratings`; read-only star ratings
- `orders` — package `org.bookinfo.orders`; order lifecycle
- `inventory` — package `org.bookinfo.inventory`; stock and availability
- `notifications` — package `org.bookinfo.notifications`; event-driven messaging
- `search` — package `org.bookinfo.search`; full-text search
- `recommendations` — package `org.bookinfo.recommendations`; discovery
- `statuspage` — package `org.bookinfo.statuspage`; aggregated health and uptime dashboard

## Preferred Service Layout

Use package-by-feature inside each service. A feature package may contain:

- `controller/` — REST endpoints and request validation
- `controller/dto/` — request/response DTOs
- `service/` — business logic and transaction boundaries
- `service/mapper/` — extension-function mappers
- `domain/` — entities, value objects, domain events, sealed result/error types
- `repository/` — Spring Data interfaces
- `config/` — `@Configuration` and `@ConfigurationProperties`
- `client/` — downstream HTTP clients
- `event/` — messaging publishers/consumers when that service introduces them
- `exception/` — `@RestControllerAdvice` and custom exception types

## Implementation Standards

- Constructor injection only.
- Wire beans in `@Configuration` classes; do not rely on `@Service`/`@Component` scanning.
- Controllers stay thin: validate input, delegate, return response.
- DTOs belong at the controller boundary; services work with entities, value objects, or domain models.
- Services own business logic and transaction boundaries.
- Repositories handle data access only; no business logic.
- Use `@Valid` and Bean Validation on request bodies.
- Use `@ConfigurationProperties` with immutable typed config; avoid `@Value`.

## Kotlin Rules

- Prefer `val` over `var`.
- Use immutable `data class` for DTOs and value objects.
- Use `sealed class` or `sealed interface` for domain result types and error hierarchies.
- Do not use an `else` branch for `when` on sealed types.
- Never use `!!`; handle nullability explicitly with `?.`, `?:`, `let`, `require`, or early return.
- Prefer extension functions for DTO-to-entity and entity-to-DTO mapping.
- Prefer `require`, `check`, and `error` over ad hoc argument validation exceptions.
- Use `io.github.oshai` Kotlin Logging; never use `println`.
- Never log PII, secrets, passwords, tokens, or other sensitive data.

## Spring and API Conventions

- OpenAPI-first for new endpoints: annotate controllers with `@Tag`, `@Operation`, and `@ApiResponse`.
- REST conventions: proper HTTP verbs, plural resource names, and standard status codes.
- Global exception handling via `@RestControllerAdvice` with typed `ProblemDetail` responses where applicable.
- Configuration lives in `application.yml`; externalize credentials, URLs, and timeouts.
- Health checks must work through Spring Boot Actuator.

## Data, Persistence, and Service Boundaries

- Parameterized queries only; never concatenate SQL.
- Repository interfaces should be `internal` when possible for encapsulation.
- Flyway migrations use `V{NN}__description.sql`; never modify released migrations.
- JPA entities are regular classes, not `data class`.
- Do not introduce new shared-database coupling between services; the target is database-per-service.
- Note: `reviews` and `ratings` currently share the `reviews` table; treat that as an existing constraint, not a pattern to extend to other services.

## Inter-Service Communication

- Prefer typed HTTP clients for downstream calls and keep the choice consistent with the module's existing style.
- Every outbound call must have explicit timeout configuration and Resilience4j circuit breaker protection.
- Propagate correlation and tracing headers via Micrometer / OpenTelemetry automatic instrumentation (Istio sidecars augment this at the mesh level).
- Kafka, RabbitMQ, Redis, OpenFeign, and Spring Cloud Config are approved patterns when explicitly introduced, but they are not mandatory defaults for every service.

## Istio Service Mesh

- Istio provides mTLS, traffic management, mesh-level authorization, and observability. Application-level patterns (Resilience4j, Spring Security, OpenTelemetry) complement but do not replace mesh features.
- All namespaces running services must have `istio-injection=enabled` for automatic sidecar injection.
- `PeerAuthentication` is STRICT mode -- all service-to-service traffic is mTLS-encrypted.
- `AuthorizationPolicy` restricts which services can call which (least-privilege mesh RBAC). This works alongside Kubernetes NetworkPolicy as defense-in-depth.
- Ingress uses Istio `Gateway` + `VirtualService` with specific hostnames (not wildcard `*`).
- `DestinationRule` per service defines subsets (versions), connection pool settings, and load balancing policy.
- Traffic splitting, fault injection, and traffic mirroring configs live in `istio/scenarios/` for teaching purposes.
- `ServiceEntry` required for any egress to external services; default mesh policy denies unregistered outbound traffic.
- Do not duplicate at the mesh level what Resilience4j already handles well at the application level (e.g., circuit breaking with state). Use Istio retries/timeouts for mesh-level defaults; use Resilience4j for application-aware circuit breaking with fallback logic.

## Testing Standards

- Unit tests: no Spring context, no tag, no Docker.
- Integration tests: `@Tag("integration")`; may use `@SpringBootTest` and Testcontainers.
- Prefer slice tests when sufficient, such as `@WebMvcTest` and `@DataJpaTest`.
- Use AssertJ for assertions and MockK for mocking.
- Test names should follow `should <expected behavior> when <condition>`.
- Use factory functions or builders for test data; avoid random data in assertions.

## Dependency Rules

- All versions must be managed in `gradle/libs.versions.toml`.
- Never hardcode versions in `build.gradle.kts`.
- Always use the latest compatible stable versions of Gradle plugins.
- Add new dependencies to the version catalog first, then reference them via `libs.<alias>`.
- Prefer version catalog bundles and BOM imports via `platform()` when available.

## Workflow Rules

- Prefer logical smaller commits over large, mixed-scope commits.
- Always run the relevant tests before handing work back; if tests cannot be run, explain why.

## Build Commands

```bash
./gradlew build                          # full build (unit + integration + detekt + jacoco)
./gradlew test                           # unit tests only (excludes @Tag("integration"))
./gradlew integrationTest                # integration tests only
./gradlew detekt                         # static analysis
./gradlew :services:<name>:test          # single module tests
./gradlew :services:<name>:bootRun       # run a single service locally
```

## Documentation Index

All project docs live under `docs/` (see [docs/README.md](docs/README.md) for an index):

- `docs/product.md` — Product vision, capabilities, roadmap
- `docs/architecture.md` — Service topology and cross-cutting architecture
- `docs/tech.md` — Stack, build layout, deployment artifacts
- `docs/modernization.md` — Modernization plan with phases
- `docs/jiraboard.md` — Ticket board (BOOK-1 through BOOK-32)
- `docs/findings.md` — Original codebase audit (28 findings)
- `istio/scenarios/` — Example Istio traffic configs (canary, fault injection, mirroring, rate limiting)
- `.cursor/skills/kotlin-springboot/SKILL.md` — Detailed Kotlin/Spring Boot conventions

## Do Not

- Do not use Java-style patterns where Kotlin has better idioms.
- Do not use `var` when `val` suffices.
- Do not use `!!`.
- Do not use field injection or setter injection.
- Do not rely on `@Service`/`@Component` when project wiring is expected in `@Configuration`.
- Do not hardcode dependency versions outside the version catalog.
- Do not concatenate SQL.
- Do not add downstream calls without timeout and circuit breaker protection.
- Do not log sensitive data.
- Do not assume an optional platform capability is already implemented in every service just because the repo supports it.
- Do not use wildcard hosts (`*`) in Istio Gateway or VirtualService resources.
- Do not bypass Istio sidecar injection for services that should be part of the mesh.
- Do not duplicate Resilience4j circuit-breaking logic in Istio DestinationRule outlier detection; use each tool for what it does best.
