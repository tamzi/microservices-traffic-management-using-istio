# Technical reference

This document summarizes **how the repository is built and run**. Naming, wiring rules, and day-to-day conventions for contributors are in [Agents.md](../Agents.md). Product goals and roadmap are in [product.md](product.md).

## Repository layout

- **Root Gradle build** — `build.gradle.kts`, `settings.gradle.kts`, and `gradle/libs.versions.toml` define shared plugins, dependency versions, and included modules under `services/`.
- **Backend service target** — Thirteen Spring Boot applications under `services/<name>/` (`productpage`, `web-bff`, `mobile-bff`, `users`, `details`, `reviews`, `ratings`, `orders`, `inventory`, `notifications`, `search`, `recommendations`, `statuspage`). Each module owns its `build.gradle.kts` and source tree as it lands.
- **Frontend target** — Two Vue.js + TypeScript SPAs: `services/storefront/` (customer-facing) and `services/admin/` (operator dashboard). Each has its own `package.json`, `vite.config.ts`, and Dockerfile. Not Gradle modules; built with npm/pnpm and served by Nginx.
- **Documentation** — `docs/` holds vision, modernization plan, task board, and this file. [architecture.md](architecture.md) describes runtime topology and boundaries.
- **Legacy** — Older polyglot samples and root-level YAML may still be present until removed per [jiraboard.md](jiraboard.md) (BOOK-19).

## Stack (source of truth)

Pinned versions live in `gradle/libs.versions.toml`. The table below mirrors the intent of [Agents.md](../Agents.md); if anything disagrees, **the version catalog and Agents.md win**.

| Concern | Technology |
| --- | --- |
| Language (backend) | Kotlin (see `kotlin` in `gradle/libs.versions.toml`) |
| Language (frontend) | TypeScript, Vue.js 3 |
| Runtime (backend) | Java 21 |
| Framework (backend) | Spring Boot, Spring Cloud BOM where used |
| Framework (frontend) | Vue.js 3 + Vite, served by Nginx |
| Build (backend) | Gradle with Kotlin DSL, version catalog |
| Build (frontend) | npm/pnpm + Vite |
| Persistence | MySQL 8.4, Flyway migrations, Spring Data JPA where applicable |
| Resilience | Resilience4j (circuit breaker, retry) on outbound HTTP calls |
| Security | Spring Security for application authorization; Istio mTLS and AuthorizationPolicy in-cluster; cert-manager for ingress TLS |
| Observability | Micrometer, OpenTelemetry-related stack aligned with Spring Boot, Actuator health |
| Static analysis | Detekt, JaCoCo (backend); ESLint, TypeScript strict mode (frontend) |
| Testing (backend) | JUnit 5/6 line per catalog, MockK, Testcontainers, AssertJ |
| Testing (frontend) | Vitest (unit), Playwright or Cypress (e2e) |
| Packaging | Multi-stage Docker images (Eclipse Temurin 21 for backend, Nginx alpine for frontend), Helm 3 charts |
| Mesh | Istio (PeerAuthentication, Gateway, VirtualService, DestinationRule, AuthorizationPolicy, ServiceEntry for egress) |
| CI/CD | GitHub Actions (workflows land per modernization milestones) |

## Build and test commands

Standard commands are documented in [Agents.md](../Agents.md) under **Build Commands**. Typical entry points:

- Full backend build — `./gradlew build`
- Unit tests only — `./gradlew test`
- Single module — `./gradlew :services:<name>:test` or `:services:<name>:bootRun`
- Storefront build, once implemented — `cd services/storefront && npm run build`
- Admin build, once implemented — `cd services/admin && npm run build`
- Frontend dev servers, once implemented — `cd services/storefront && npm run dev` or `cd services/admin && npm run dev`

Integration tests use the tag convention described in Agents.md (`@Tag("integration")`).

## Configuration

- **Application config** — Per-service `application.yml` (or profile-specific variants) under each module's `src/main/resources/`. Secrets must not be committed; use Kubernetes Secrets or an external secret operator pattern in deployment docs.
- **Database** — Connection properties and Flyway locations are defined per service as implementation completes (see [jiraboard.md](jiraboard.md) BOOK-2 onward).
- **Frontend config** — Environment-specific API base URL configured via Vite env files (`.env`, `.env.production`) or Nginx reverse proxy config. Both `storefront` and `admin` point to the `web-bff` API; mobile clients point to `mobile-bff`.

## Deployment artifacts

- **Containers** — One image per service; Eclipse Temurin 21 base for backends, Nginx alpine for both frontends. Non-root user requirements are in the modernization plan and Agents.md.
- **Kubernetes** — Helm chart under `helm/bookinfo/` covering all fifteen services; Istio resources may live under chart templates or `istio/` as the repo converges.

## License

Apache License 2.0 (see repository `LICENSE` if present at root).
