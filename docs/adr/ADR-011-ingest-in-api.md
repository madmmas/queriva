# ADR-011 — Ingest Orchestration: Spring Boot over Separate Python Service

## Status
Accepted

## Context
The ingest pipeline (chunk → embed → upsert) could live in either:

1. **Spring Boot API** (same process as the search gateway)
2. **A separate Python service** (co-located with the embed sidecar and ingest-cli,
   where Python ML libraries already exist)

The embed sidecar is already Python. The ingest-cli is Python. Moving ingest
orchestration to Python would mean three Python components vs one Java component
handling ingest.

## Decision
Implement ingest orchestration (`IngestController`, `IngestService`,
`ChunkingService`, `CollectionManager`) inside the **Spring Boot API package**,
alongside the search gateway.

## Rationale

### Search path symmetry
Both ingest and search share the same Qdrant connection and the same embed
sidecar calls. Keeping them in one Spring Boot application means:
- One Qdrant client configuration
- One `application.properties` / env config
- One Docker service (`api`) for consumers to operate

### ChunkingService has no ML dependencies
The chunking logic (ADR-003, ADR-007) is pure string manipulation — no ML
libraries needed. There is no technical reason to move it to Python. Implementing
it in Java keeps the JVM layer self-contained for its core responsibilities.

### Fewer network hops in the ingest path
If ingest orchestration were a separate Python service:
```
ingest-cli → Python ingest service → embed-sidecar → Qdrant
```
With ingest in Spring Boot:
```
ingest-cli → api (Spring Boot) → embed-sidecar → Qdrant
```
Same number of hops. No benefit to the extra Python service.

### Single deployable for API consumers
Consumers who call `POST /api/ingest/documents` and `POST /api/search` talk
to one service (`localhost:8080`). A separate Python ingest service would
require consumers to know two base URLs and manage two services.

## Alternatives Considered

| Option | Reason rejected |
|---|---|
| **Python ingest microservice** | Extra Docker service, extra base URL, extra health check. No technical advantage given ChunkingService has no ML dependencies. |
| **Ingest only in ingest-cli (no API)** | CLI is for batch offline ingestion. Some consumers (News Radar) need to push via HTTP (§7.6). CLI-only ingest removes the API push pattern. |

## Consequences

**Makes easier:**
- One API base URL for all operations — simpler DX for consumers
- `IngestService` and `SearchService` share the same Qdrant client bean
- Health endpoint reflects ingest dependencies (embed-sidecar, Qdrant) in one place

**Makes harder:**
- Spring Boot API Docker image grows slightly (ingest + search code)
- Java `ChunkingService` must be kept in sync if chunking logic evolves
  alongside Python's `chunker.py` in ingest-cli (they use the same algorithm
  but are separate implementations)

## References
- SPEC.md §4 (architecture diagram), §5 (repo structure), §7.1 (ingestion flow)
- Issue #5 (API skeleton), #6 (ChunkingService), #7 (IngestService)
- ADR-004 (Spring Boot — the layer this decision extends)
- ADR-005 (embed sidecar — the Python component ingest calls for embeddings)
