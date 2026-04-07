# Project Analysis: Microservices Traffic Management Using Istio

## Overview

This is an IBM Code Pattern demonstrating Istio service mesh capabilities using a modified BookInfo application. The project deploys a polyglot microservices architecture onto Kubernetes (IBM Cloud / Bluemix) with Istio handling traffic management, observability, and egress control for an external MySQL database.

**License:** Apache 2.0
**Target Platform:** IBM Cloud Kubernetes Service (formerly Bluemix Container Service)
**Istio Version:** 1.0.2 (install script), with some artifacts referencing pre-1.0 APIs

---

## Architecture

### Service Topology

```
Internet
  │
  ▼
Istio IngressGateway (port 80)
  │
  ├── /productpage, /login, /logout, /api/v1/products
  │     └── productpage (Python, pre-built image only)
  │           ├── GET /details  ──► details (Ruby)  ──► MySQL
  │           ├── GET /reviews  ──► reviews (Java)   ──► MySQL
  │           │                       └── GET /ratings ──► ratings (Node.js) ──► MySQL
  │           └── POST /postReview, GET /deleteReviews ──► reviews ──► MySQL
  │
  └── MySQL (in-cluster or external via ServiceEntry)
```

### Microservices Inventory

| Service | Language | Framework | Port | Source in Repo |
|---------|----------|-----------|------|----------------|
| productpage | Python | Flask (assumed) | 9080 | **Not included** -- uses pre-built image `journeycode/productpage-orig` |
| details | Ruby 2.3 | WEBrick | 9080 | `microservices/details/details.rb` |
| ratings | Node.js 4 | http + httpdispatcher | 9080 | `microservices/ratings/ratings.js` |
| reviews | Java 8 | JAX-RS / Open Liberty | 9080 | `microservices/reviews/` (Gradle multi-project) |
| book-database | MySQL 5.6 | -- | 3306 | `microservices/bookinfo_db/` |
| mysql-data (setup) | MySQL 8 | -- | 3306 | `microservices/mysql_data/` |

### Database Schema

Both `bookinfo_db` SQL init scripts define:
- `books` table: BookID, Author, Year, Title, Paperback, Publisher, Language, ISBN_10, ISBN_13
- `reviews` table: ReviewID (auto-increment), BookID, Reviewer, Review, Rating

---

## Istio Configuration

### Traffic Management

| Kind | File | Purpose |
|------|------|---------|
| Gateway | `bookinfo-gateway.yaml`, `istio-gateway.yaml` | HTTP ingress on port 80, wildcard hosts |
| VirtualService | `bookinfo-gateway.yaml` | Routes to productpage + reviews (includes `/postReview`, `/deleteReviews`) |
| VirtualService | `istio-gateway.yaml` | Routes to productpage only (no review mutation routes) |
| ServiceEntry | `mysql-egress.yaml` | Allows egress to external MySQL at `sl-us-south-1-portal.38.dblayer.com:59454` |

### Observability (Mixer-based, deprecated)

`new-metrics-rule.yaml` defines:
- Custom Prometheus metric `double_request_count` (counts each request twice as a demo)
- Custom stdio log entry with source, destination, response code, size, latency

### Missing Istio Resources

The following are referenced in the README but not included in the repository (they come from Istio's upstream `samples/bookinfo/networking/`):
- DestinationRule
- Fault injection rules
- Circuit breaker configuration
- Rate limiting policies
- mTLS policies

---

## Findings

### Critical Issues

**1. SQL Injection Vulnerability (reviews service)**

`LibertyRestEndpoint.java` line 174 builds SQL via string concatenation with unsanitized user input:

```java
String sql = ("INSERT INTO reviews (BookID,Reviewer,Review,Rating) VALUES (\"1\",\""
    + reviewerForm + "\",\"" + reviewForm + "\",\"" + ratingForm + "\")");
```

Form inputs `reviewer`, `review`, and `rating` are injected directly into the query. This is exploitable. Must use `PreparedStatement` with parameterized queries.

**2. Destructive Endpoint Exposed Without Authentication**

`GET /deleteReviews` wipes the entire `reviews` table (`DELETE FROM reviews`) with no authentication, authorization, or confirmation. This is accessible via the Istio gateway in `bookinfo-gateway.yaml`.

**3. Secrets Committed to Source Control**

`secrets.yaml` contains base64-encoded credentials:
- `username`: `admin` (decoded from `YWRtaW4=`)
- `password`: decoded from `T0NVUUhDQ1NKT0JEVEtUWQ==`
- `host`: `sl-us-south-1-portal.38.dblayer.com`
- `port`: `59454`

Even if these are demo values, committing secrets to Git is a pattern that should never be demonstrated. Use `SealedSecrets`, `ExternalSecrets`, or Vault in examples.

**4. Hardcoded Database Credentials in Install Script**

`scripts/install.sh` (lines 29-32) hardcodes `book_user` / `password` and patches `secrets.yaml` with `sed` using plain-text base64 values.

### Security Concerns

**5. No TLS Termination**

Both Gateway resources serve HTTP only (port 80). No TLS configuration exists anywhere in the project.

**6. Wildcard Host Matching**

Gateways use `hosts: ["*"]`, accepting traffic for any hostname. Production deployments should restrict to specific domains.

**7. No Network Policies**

No Kubernetes NetworkPolicy resources restrict pod-to-pod communication. The mesh relies entirely on Istio sidecar enforcement, but there's no `PeerAuthentication` or `AuthorizationPolicy` to enforce mTLS or RBAC.

**8. MySQL Root Password in Plain Text**

`book-database.yaml` contains `MYSQL_ROOT_PASSWORD: password` as a plain environment variable (not even referenced from a Secret).

### Outdated / Deprecated Components

**9. Deprecated Istio Mixer Telemetry**

`new-metrics-rule.yaml` uses `config.istio.io/v1alpha2` CRDs (metric, prometheus, rule, logentry, stdio). Mixer was deprecated in Istio 1.7 and removed in 1.12. These resources will not function on any modern Istio installation.

**10. Deprecated Kubernetes API Versions**

`bookinfo.yaml` uses `apiVersion: extensions/v1beta1` for Deployments. This API was removed in Kubernetes 1.16 (September 2019). Must use `apps/v1` with a `selector` field.

**11. Severely Outdated Base Images**

| Dockerfile | Base Image | Status |
|------------|-----------|--------|
| details | `ruby:2.3` | EOL since March 2019 |
| ratings | `node:4-onbuild` | EOL since April 2018, `onbuild` tags removed |
| reviews | `openliberty/open-liberty:microProfile1-java8-openj9` | MicroProfile 1.x is legacy |
| mysql_data | `mysql:8` | No version pin -- pulls latest 8.x |
| bookinfo_db | `mysql:5.6` | EOL since February 2021 |

**12. Outdated Dependencies**

- `mysql-connector-java:5.1.6` (reviews Gradle) -- released 2007, renamed to `com.mysql:mysql-connector-j`, current is 9.x
- `httpdispatcher:1.0.0` (ratings npm) -- last published 2014
- `mysql:2.13.0` (ratings npm) -- unmaintained, superseded by `mysql2`
- `com.mysql.jdbc.Driver` class (reviews Java) -- deprecated, replaced by `com.mysql.cj.jdbc.Driver`

**13. Legacy IBM Cloud CLI**

Scripts reference `bx` (Bluemix CLI) which was renamed to `ibmcloud` years ago. The Bluemix CLI download URL (`public.dhe.ibm.com/cloud/bluemix/cli/bluemix-cli/Bluemix_CLI_0.6.1_amd64.tar.gz`) is likely dead.

### Code Quality Issues

**14. Duplicated YAML Block**

`new-metrics-rule.yaml` contains the exact same 84-line configuration block duplicated (lines 1-84 are identical to lines 85-168). Applying this will produce Kubernetes resource name conflicts.

**15. Database Connection Leaks**

- `details.rb`: Creates a new MySQL connection on every `/details` request and never closes it.
- `ratings.js`: Calls `connection.end()` synchronously after an async `connection.query()` -- the connection may close before the query callback fires.
- `LibertyRestEndpoint.java`: The `getRatings` method creates a new JAX-RS `Client` on every call without closing it, leaking HTTP connections. The `postReview` and `deleteReview` methods don't close JDBC `Connection` or `Statement` in a `finally` block.

**16. Global Mutable State (ratings.js)**

Rating variables (`first_rating` through `fifth_rating` and `ratingsResponse`) are module-level globals mutated inside async callbacks. Under concurrent requests, these will produce race conditions and incorrect responses.

**17. Hardcoded Array Size (reviews Java)**

`LibertyRestEndpoint.java` allocates fixed arrays of size 5 for reviews and ratings, with a hard `break` at 5. This silently drops data and will `ArrayIndexOutOfBoundsException` if the assumption is violated.

**18. Missing Health Check for Database Connectivity**

All three services expose `/health` endpoints that return 200 unconditionally, without verifying database connectivity. This means Kubernetes liveness/readiness probes will report healthy even when the database is down.

**19. No Input Validation**

- The `rating` form field accepts any string, but the database column and the star-rendering logic expect integers 1-5.
- No request size limits on the `review` text area (maxlength is only client-side HTML).
- The details service eagerly crashes on startup if `MYSQL_DB_PORT` env var is missing (Ruby `Integer()` on nil).

### Deployment & Operations

**20. No Readiness/Liveness Probes in YAML**

None of the Kubernetes Deployment manifests define `readinessProbe` or `livenessProbe`, despite all services exposing `/health` endpoints. Kubernetes will route traffic to containers that haven't finished starting.

**21. No Resource Limits**

No `resources.requests` or `resources.limits` defined on any container. A single misbehaving pod can consume all node resources.

**22. No Horizontal Pod Autoscaler**

All deployments are fixed at `replicas: 1` with no HPA configuration.

**23. Manual Sidecar Injection**

The install script uses `istioctl kube-inject` rather than automatic sidecar injection via namespace labeling (`istio-injection=enabled`). This is fragile and version-dependent.

**24. Missing `images/` Directory**

The README references screenshots from an `images/` directory that does not exist in the repository.

**25. Travis CI Does Full Deploy-and-Destroy**

`.travis.yml` installs Istio on a real cluster, deploys the full app, runs a health check, then tears everything down. The actual test is just an HTTP 200 from `/productpage` -- there are no unit tests, integration tests, or contract tests.

### Inconsistencies

**26. Two Gateway Files With Overlapping Purpose**

`bookinfo-gateway.yaml` and `istio-gateway.yaml` both define a Gateway named `bookinfo-gateway` and a VirtualService named `bookinfo`, but with different route sets. Applying both will cause conflicts. The install script only uses `istio-gateway.yaml`.

**27. secrets.yaml Base64 Values Don't Match sed Replacements**

`secrets.yaml` has one set of base64 values, but `scripts/install.sh` replaces different base64 strings (e.g., `VEhYTktMUFFTWE9BQ1JPRA==` which doesn't match the committed `T0NVUUhDQ1NKT0JEVEtUWQ==`). The sed operation will silently fail, leaving the original (external DB) credentials in place instead of the intended in-cluster values.

**28. Inconsistent Environment Variable Naming**

Services expect `MYSQL_DB_HOST`, `MYSQL_DB_PORT`, `MYSQL_DB_USER`, `MYSQL_DB_PASSWORD`, but the Kubernetes Secret keys are `host`, `port`, `username`, `password`. The `-new.yaml` deployment files map Secret keys to the correct env var names, but this mapping is easy to break.

---

## Summary

| Category | Count |
|----------|-------|
| Critical (security) | 4 |
| Security concerns | 4 |
| Deprecated / outdated | 5 |
| Code quality | 6 |
| Deployment / ops | 6 |
| Inconsistencies | 3 |
| **Total** | **28** |

This project serves as an educational code pattern from ~2017-2018 and was not designed for production use. However, many of the issues (SQL injection, committed secrets, deprecated APIs) make it unsuitable as a learning reference without significant modernization. A modern equivalent would use Istio 1.17+, `apps/v1` Deployments, Istio Telemetry v2 (Envoy-native), proper secret management, and parameterized database queries.
