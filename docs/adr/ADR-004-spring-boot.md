# ADR-004 — API Gateway Language: Spring Boot / Java 21

## Status
Accepted

## Context
The Queriva API gateway orchestrates two complex flows: ingest (chunk → embed →
upsert) and search (embed → search → synthesize). It needs a mature HTTP
framework, a strong Qdrant client, good testability with Testcontainers, and
language consistency with the broader AIPlane ecosystem (which is also Spring Boot).

## Decision
Use **Spring Boot 3.x with Java 21** for both the ingest and search API packages.

## Alternatives Considered

| Option | Reason rejected |
|---|---|
| **Go** | Used in News Radar's pipeline (proven). But AIPlane is Spring Boot — keeping one JVM language across AIPlane + Queriva simplifies shared type contracts and developer context switching. Go's Qdrant client is less mature than the Java client. |
| **FastAPI (Python)** | Natural choice given the embed sidecar is already Python. But mixing Python orchestration with Java would create two heavy runtimes. Java 21 virtual threads remove the async throughput argument for Python. |
| **Quarkus** | Faster startup, native image support. But smaller ecosystem for Testcontainers + Qdrant. Spring Boot has wider Upwork client recognition. |
| **Node.js / Express** | Weak typing at scale. Not aligned with enterprise Java positioning. |

## Java 21 Specifics
- **Virtual threads** (`--enable-preview` not needed in Java 21 GA) eliminate the
  need for reactive programming (WebFlux) for concurrent embed sidecar calls
- `@Async` + `CompletableFuture` for batched embedding in `IngestService`
- Spring Boot 3.x requires Java 17 minimum — Java 21 LTS is the natural choice

## Consequences

**Makes easier:**
- Spring Boot Test + Testcontainers = excellent integration test ergonomics (ADR-009)
- `io.qdrant:qdrant-client` (official Java gRPC client) is first-class
- AIPlane can import Queriva's DTOs/types as a shared Maven dependency in the future
- CORS config, health endpoints, and env-based config are idiomatic in Spring Boot

**Makes harder:**
- JVM cold start (~3–5s) vs Go (<1s) — acceptable for long-running service
- Heavier Docker image than Go binary (~200MB vs ~20MB)
- pom.xml dependency management is more verbose than Go modules

## References
- SPEC.md §4 (component responsibilities), §16 (tech stack)
- Issue #5 (API skeleton), #7 (IngestService), #14 (SearchController)
- ADR-011 (why ingest lives in Spring Boot rather than a Python service)
