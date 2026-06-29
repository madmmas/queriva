# ADR-005 — Embedding Sidecar: Separate FastAPI Process

## Status
Accepted

## Context
Queriva needs to generate sentence embeddings at two points: during ingest
(for each document chunk) and during search (for each user query). The
sentence-transformers library is Python-native. The API gateway is Java.
A decision is needed on how to make embeddings available to the JVM.

## Decision
Run embeddings in a **separate FastAPI process** (the "embed sidecar"), exposing
a minimal REST API (`POST /api/embed`). The Spring Boot API calls this sidecar
over HTTP for both ingest and search paths.

## Alternatives Considered

| Option | Reason rejected |
|---|---|
| **ONNX Runtime in JVM** | LaBSE can be exported to ONNX, but the export process is non-trivial and model-specific. Tokenizer logic (especially for multilingual models) must also be ported. Maintenance burden is high when updating models. |
| **DJL (Deep Java Library)** | Supports sentence-transformers models but community support is thinner than sentence-transformers Python. Tokenizer parity for LaBSE/Bangla is unverified. |
| **gRPC sidecar** | Lower overhead than REST but adds protobuf schema management. REST is sufficient at Queriva's throughput targets. |
| **Embed in ingest-cli only** | Would require the CLI to be running during search — not viable for standalone API operation. |

## Design
The sidecar:
- Lazy-loads models on first request, caches in memory
- Supports multiple models simultaneously (LaBSE + MiniLM can coexist)
- Used by both `IngestService` (batched, `INGEST_BATCH_SIZE` chunks per call)
  and `QueryEmbeddingService` (single query per call)
- Has its own health endpoint (`GET /api/health`)
- Is the **only** component that touches Python ML libraries

```
IngestService      → HTTP → embed-sidecar:8001/api/embed (batched)
QueryEmbeddingService → HTTP → embed-sidecar:8001/api/embed (single)
```

## Consequences

**Makes easier:**
- Python ML ecosystem (sentence-transformers, HuggingFace) used natively
- Model updates require only sidecar changes — no JVM redeployment
- Sidecar is independently scalable (run two instances behind a load balancer in v2)
- News Radar already runs LaBSE in Python — the sidecar reuses that proven setup

**Makes harder:**
- Additional network hop per query adds ~5–15ms latency
- Sidecar failure degrades both ingest and search — health check + graceful
  degradation required in the API (already in `GET /api/health`)
- Two Docker services to manage vs one monolithic JVM process

## References
- SPEC.md §9 (embedding sidecar), §6 (API contract: POST /api/embed)
- Issue #2 (embed sidecar implementation), #13 (QueryEmbeddingService)
- ADR-002 (LaBSE — the primary model the sidecar hosts)
- ADR-004 (Spring Boot — explains why the gateway is Java)
