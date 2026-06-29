# ADR-009 — Testing Strategy: Testcontainers for Qdrant Integration Tests

## Status
Accepted

## Context
The API layer has two services that talk to Qdrant: `CollectionManager` and
`QdrantSearchService`. Integration tests for these services need a running
Qdrant instance. The two main options are:

1. **Testcontainers:** spin up a real `qdrant/qdrant` Docker container per test run
2. **Mock Qdrant client:** mock the `io.qdrant:qdrant-client` interfaces in unit tests

## Decision
Use **Testcontainers** with the official `qdrant/qdrant` Docker image for all
integration tests that touch Qdrant. Unit tests for business logic (e.g.
`ChunkingService`, `SearchResultMapper`) use plain JUnit 5 with no containers.

## Rationale

### Why not mock the Qdrant client?

Qdrant's Java client is a gRPC/REST thin wrapper with minimal abstraction.
Mocking it would:
- Test mock behaviour, not real Qdrant behaviour
- Give false confidence that payload filters work correctly
- Miss Qdrant version-specific quirks (filter DSL changes between Qdrant versions)
- Not catch issues with collection creation parameters (vector_size, distance metric)

The most valuable tests for `QdrantSearchService` are ones that verify:
- "Does cosine similarity filter correctly at min_score=0.60?"
- "Does the language payload filter restrict results?"
- "Does skip_existing upsert mode actually not duplicate points?"

None of these can be verified against a mock.

### Testcontainers setup
```java
@Testcontainers
class QdrantSearchServiceIT {

    @Container
    static QdrantContainer qdrant = new QdrantContainer("qdrant/qdrant:latest");

    // or via generic container:
    @Container
    static GenericContainer<?> qdrant = new GenericContainer<>("qdrant/qdrant:latest")
        .withExposedPorts(6333, 6334);
}
```

Testcontainers starts a fresh Qdrant instance per test class, ensuring test
isolation with no shared state between test runs.

### Test tagging strategy
```java
@Tag("unit")        // no Docker, fast, run always
@Tag("integration") // requires Docker, slower, run in CI and pre-commit
@Tag("slow")        // Ollama RAG tests — run nightly or manually
```

`make test-unit` runs only `@Tag("unit")`.
`make test-int` runs `@Tag("integration")` (requires Docker).
`make test` runs both unit and integration.

## Consequences

**Makes easier:**
- Integration tests catch real Qdrant API behaviour — including filter bugs,
  index configuration issues, and client version mismatches
- Test isolation: each test class gets a fresh Qdrant instance
- CI (GitHub Actions `ubuntu-latest`) has Docker available — Testcontainers works without changes

**Makes harder:**
- Integration tests require Docker at test time — documented in README
- First run pulls `qdrant/qdrant` image (~200MB) — CI caches the layer after first pull
- Integration tests are slower (~10–30s startup) — separated from unit tests via tags

## References
- SPEC.md §16 (tech stack — JUnit 5, Testcontainers)
- Issue #1 (test infrastructure), #5 (collection management tests),
  #8 (ingest integration tests), #12 (QdrantSearchService tests), #15 (search integration tests)
- ADR-001 (Qdrant — the system being tested)
