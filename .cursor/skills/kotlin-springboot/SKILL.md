---
name: kotlin-springboot
description: >-
  Kotlin + Spring Boot conventions for the BookInfo microservices repo.
  Use when creating or modifying Kotlin source files, Spring Boot services,
  REST controllers, repositories, tests, or Gradle build files in this project.
  Applies to any work under services/, build.gradle.kts, or gradle/.
---

# Kotlin + Spring Boot Conventions (BookInfo)

## Stack

- Kotlin 2.3, Spring Boot 4.0, Java 21, Gradle 9 (Kotlin DSL)
- JUnit 6, MockK, Testcontainers 2, AssertJ
- Flyway for migrations, Resilience4j for circuit breaking
- Detekt for static analysis, JaCoCo for coverage
- All dependency versions live in `gradle/libs.versions.toml`

## Project Layout

```
services/<name>/
├── build.gradle.kts
├── Dockerfile
└── src/
    ├── main/kotlin/org/bookinfo/<name>/
    │   ├── <Name>Application.kt
    │   ├── <feature>/
    │   │   ├── <Feature>Controller.kt
    │   │   ├── <Feature>Service.kt
    │   │   ├── <Feature>Repository.kt
    │   │   ├── <Feature>Configuration.kt
    │   │   └── dto/
    │   └── ...
    ├── main/resources/
    │   └── application.yml
    └── test/kotlin/org/bookinfo/<name>/
        ├── <Feature>UnitTest.kt        (no @Tag)
        └── <Feature>IntegrationTest.kt (@Tag("integration"))
```

- Package-by-feature: each feature gets its own package with controller, service, repository, config, and DTOs
- Never mix features in the same package
- All services listen on port `9080`

## Kotlin Style

### Idioms
- `val` over `var`; `data class` for DTOs; `sealed class` for type hierarchies
- Scope functions: `let` for nullables, `apply` for object config, `also` for logging
- Expression body for single-expression functions; omit return type when obvious
- `when` over `if-else` chains; collection operators over imperative loops
- `_` for unused lambda params and caught exceptions: `catch (_: IllegalArgumentException)`
- Named arguments when a function has 3+ parameters

### Null Safety
- Never use `!!`; handle nulls with `?.`, `?:`, `let`, or early return
- Nullable types only when the domain genuinely allows null

### Value Classes
```kotlin
@JvmInline
value class BookId(val value: Long)
```
Use for type-safe IDs and domain primitives. Parse strings at the controller boundary.

### Package-Level Functions
Prefer top-level factory functions over companion objects (matches stdlib style like `listOf()`).

## Spring Boot Conventions

### Bean Wiring
- Constructor injection only; never field injection
- Define beans in `@Configuration` classes, not via `@Service` / `@Repository` / `@Component`
- Mark `@Configuration` classes and `@Bean` / `@Transactional` methods as `open`

### REST Controllers
- DTOs for request/response; suffix with `Dto`
- `@Valid` + Bean Validation on request bodies
- HTTP status codes: 201 Created, 400 Bad Request, 404 Not Found, 409 Conflict, 503 Service Unavailable
- Exception handling via `@ControllerAdvice`

### Data Access
- Spring Data JPA; parameterized queries only (never string-concatenated SQL)
- HikariCP connection pool (Spring Boot default)
- Flyway migrations at `src/main/resources/db/migration/V<YYYYMMDD>__description.sql`
- Repository interfaces marked `internal` to enforce encapsulation

### Configuration
- `application.yml` (not `.properties`)
- `@ConfigurationProperties` with constructor binding for typed config
- Externalize DB credentials, service URLs, and timeouts via environment variables

### HTTP Clients
- `WebClient` (reactive) or `RestClient` (blocking) for inter-service calls
- Always set connection + read timeouts via `@ConfigurationProperties`
- Resilience4j circuit breaker + retry on all downstream calls
- Propagate tracing context automatically via Micrometer / OpenTelemetry

### Logging
- `private val logger = KotlinLogging.logger {}` (kotlin-logging-jvm)
- INFO on writes, DEBUG on reads, WARN on recoverable errors, ERROR on fatal
- `.also { logger.debug { "Found ${it.size} items" } }` for post-action logging
- Never log PII or user identifiers

### Health & Observability
- Spring Boot Actuator: `/actuator/health`, `/actuator/readiness`, `/actuator/liveness`
- Actuator DB health indicator auto-enabled with JPA starter
- Micrometer + OpenTelemetry for traces; Prometheus registry for metrics
- Graceful shutdown enabled

## Testing

### Test Separation
- **Unit tests** (`./gradlew test`): no Spring context, no Docker, MockK for dependencies. No tag needed.
- **Integration tests** (`./gradlew integrationTest`): `@Tag("integration")`, may use `@SpringBootTest` and Testcontainers. Requires Docker.
- `check` depends on both; `test` excludes `integration` tag; `integrationTest` includes it.

### Assertions
Use AssertJ, not JUnit assertions:
```kotlin
assertThat(list).hasSize(3)
assertThat(result).isNotNull
assertThat(list).contains(item)
```

### Mocking
MockK for all mocks:
```kotlin
val service = mockk<ReviewService>()
every { service.findAll() } returns emptyList()
verify { service.findAll() }
```

### Controller Tests
- `MockMvc` + `@WebMvcTest` for unit-level controller tests
- Validate input rejection (400), not-found (404), success (200/201)

### Repository Tests
- `@Tag("integration")` + `@DataJpaTest` + Testcontainers MySQL
- Extend a shared `RepositoryIT` base if available

### Test Data
- Factory functions: `fun createBook(id: Long = 1, title: String = "Test") = Book(id, title)`
- Never use reflection to set entity fields

## Build Commands

```bash
./gradlew build              # compile + unit tests + integration tests + detekt + jacoco
./gradlew test               # unit tests only (excludes @Tag("integration"))
./gradlew integrationTest    # integration tests only (includes @Tag("integration"))
./gradlew detekt             # static analysis
./gradlew :services:details:test  # single module unit tests
./gradlew clean build        # full clean rebuild
```

## Adding a New Feature

1. Create a package under `services/<service>/src/main/kotlin/org/bookinfo/<service>/<feature>/`
2. Add controller, service, repository, configuration, and DTOs
3. Wire beans in `<Feature>Configuration.kt`
4. Add Flyway migration if schema changes
5. Write unit test (no tag) + integration test (`@Tag("integration")`)
6. Run `./gradlew :services:<service>:test` then `./gradlew :services:<service>:integrationTest`

## Adding a New Service Module

1. Create `services/<name>/build.gradle.kts` (copy from `services/details/build.gradle.kts`)
2. Add `"services:<name>"` to `settings.gradle.kts` includes
3. Create `<Name>Application.kt` with `@SpringBootApplication`
4. Create `application.yml` with `spring.application.name` and `server.port: 9080`
5. Create context-loads integration test with `@Tag("integration")`
6. Run `./gradlew :services:<name>:build`
