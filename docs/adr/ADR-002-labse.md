# ADR-002 — Embedding Model Default: LaBSE

## Status
Accepted

## Context
Queriva's embed sidecar must support at least one embedding model out of the
box. The primary production use case (News Radar) requires multilingual support
for Bangla and English. The default model must be self-hostable, produce
high-quality semantic embeddings for short-to-medium news text, and work well
with cosine similarity search in Qdrant.

The choice also determines vector dimensionality (and therefore Qdrant
collection `vector_size`), which is a collection-level setting that cannot be
changed after creation.

## Decision
Use **LaBSE** (Language-agnostic BERT Sentence Embeddings) as the default
embedding model, producing 768-dimensional vectors with cosine distance.

## Alternatives Considered

| Option | Dims | Reason rejected / kept as secondary |
|---|---|---|
| `all-MiniLM-L6-v2` | 384 | English-only. Fast and efficient but cannot handle Bangla. Supported as secondary model. |
| `paraphrase-multilingual-mpnet-base-v2` | 768 | Good multilingual support but weaker cross-lingual alignment than LaBSE. Supported as secondary model. |
| OpenAI `text-embedding-3-small` | 1536 | External API — violates local-first principle (§2). Rejected. |
| `e5-multilingual-large` | 1024 | Strong performance but higher memory footprint than LaBSE. May revisit for v2. |

## Production Validation
LaBSE was validated in the News Radar pipeline with the following results:

- Same-event average cosine similarity: **0.836**
- Different-event average cosine similarity: **0.374**
- Cosine separation gap: **0.462**
- Calibrated thresholds: REJECT < 0.60, AUTO_ACCEPT > 0.80, LLM judge 0.60–0.80

The geo_district location anchor repetition technique (repeating location 3× at
input start) was validated to significantly improve same-location article clustering.
Queriva inherits this institutional knowledge via ADR-003's title prepending.

## Consequences

**Makes easier:**
- One model handles both Bangla and English queries without language detection
- 768-dim is the standard for multilingual models — Qdrant collection config is predictable
- sentence-transformers library loads LaBSE natively — no custom inference code

**Makes harder:**
- 768-dim vectors use more storage than 384-dim MiniLM
- LaBSE model download is ~1.8GB — first startup is slow (document in README)
- Collections created with LaBSE (768-dim) cannot be queried with MiniLM (384-dim)
  without recreation — model name must be stored in collection metadata

## References
- SPEC.md §9 (embedding sidecar), §14 (Qdrant payload schema)
- Issue #2 (embed sidecar implementation), #13 (QueryEmbeddingService)
- ADR-005 (why FastAPI sidecar rather than ONNX in JVM)
- ADR-007 (character-based chunking — related to token limit awareness)
