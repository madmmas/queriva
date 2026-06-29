# ADR-001 — Vector Database: Qdrant

## Status
Accepted

## Context
Queriva needs a self-hosted vector database for storing and querying document
embeddings. Requirements: cosine similarity search, payload-based filtering
(language, date, category), persistent Docker volume, gRPC or REST API
consumable from Java (Spring Boot), and production-proven ANN performance.

## Decision
Use **Qdrant** (self-hosted via Docker) as the sole vector store for v1.

## Alternatives Considered

| Option | Reason rejected |
|---|---|
| **Weaviate** | Built-in RAG/generative module is compelling but couples vector search to LLM — Queriva keeps these layers separate. Schema management is heavier. |
| **Milvus** | Excellent at billion-scale but operationally complex (etcd, MinIO dependencies). Overkill for v1. |
| **pgvector** | Attractive given Spring Boot's native PostgreSQL support, but ANN performance degrades without HNSW tuning and payload filtering is SQL — less ergonomic than Qdrant's native filter DSL. |
| **Chroma** | Good DX but primarily Python-native. Java client is community-maintained and immature. |
| **Redis VSS** | Requires Redis Stack; payload filter model is less expressive than Qdrant's. |

## Consequences

**Makes easier:**
- Payload filters (language, date_from, date_to, category) are first-class Qdrant features — no post-filter in application code
- Java client (`io.qdrant:qdrant-client`) is officially maintained
- Testcontainers image (`qdrant/qdrant`) available — clean integration test setup (see ADR-009)
- Single Docker service, no sidecar dependencies

**Makes harder:**
- v2 multi-store support requires abstracting `QdrantSearchService` behind an interface
- Qdrant's gRPC API version pinning requires attention on upgrades

## References
- SPEC.md §4 (architecture), §12 (Docker Compose)
- Issue #4 (Qdrant Docker service), #5 (collection management), #12 (QdrantSearchService)
- ADR-009 (Testcontainers testing strategy)
