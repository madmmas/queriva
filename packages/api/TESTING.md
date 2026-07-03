# API test conventions

See ADR-009 and `test-quality.mdc` Section B.

## Tags

| Tag | When to use | Makefile target |
|---|---|---|
| `@Tag("unit")` | No Docker, fast, business logic only | `make test-unit` |
| `@Tag("integration")` | Requires Docker (Testcontainers Qdrant) | `make test-int` |
| `@Tag("slow")` | Ollama RAG tests — nightly or manual | `make test-slow` |

Every test class must have exactly one of `unit` or `integration` (or `slow`).

## Run commands

```bash
make test-api          # all API tests
make test-unit         # unit only (from repo root)
make test-int          # integration only — Docker required
make test-slow         # slow RAG tests — Docker + Ollama required
cd packages/api && mvn test -Punit
cd packages/api && mvn test -Pslow
```

## HTTP client unit tests

Use WireMock (`WireMockExtension`) to stub embed-sidecar and Ollama responses.
Never call real external services in unit tests.

## Qdrant integration tests

Use `QdrantTestcontainersSupport` with `qdrant/qdrant:latest` — same image and
ports (6333 REST, 6334 gRPC) as `docker-compose.yml`. Each test class gets a
fresh container via `@Container` + `@Testcontainers`.
