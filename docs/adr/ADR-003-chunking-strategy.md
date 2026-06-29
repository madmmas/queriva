# ADR-003 — Chunking Strategy: Sliding Window with Title Prepending

## Status
Accepted

## Context
Long documents must be split into smaller units before embedding, because
LaBSE has a 512-token input limit and embedding an entire article as one unit
degrades retrieval precision. The chunking strategy directly affects search
quality: chunks that are too large blur the embedding signal; chunks that are
too small lose context.

Queriva's primary use case (news articles, 300–2000 words) and secondary use
cases (PDF reports, markdown documents) require a strategy that works well
across varying document lengths.

## Decision
Use a **sliding window character-based chunker with title prepending**:

- Default `chunk_size = 512` characters (see ADR-007 for char vs token choice)
- Default `overlap = 64` characters between consecutive chunks
- The document **title is prepended** to every chunk before embedding:
  `"{title}. {body_slice}"`
- All chunks from one document share the same `document_id` payload field
- `body_snippet` stored in Qdrant payload = first 500 chars of chunk body (no title prefix)

## Alternatives Considered

| Option | Reason rejected |
|---|---|
| **Sentence splitting** | Sentence boundary detection is language-dependent and unreliable for Bangla. Produces very short chunks that lose context. |
| **Paragraph splitting** | Paragraph length is inconsistent in scraped web content. Some paragraphs are 50 chars, some are 1500 chars. |
| **Fixed-size no overlap** | Adjacent chunks lose cross-boundary context. Query terms at chunk boundaries are missed. |
| **Recursive character splitter (LangChain style)** | Good DX but adds LangChain dependency to Java layer. The sliding window is simpler to own. |
| **Single embedding per document** | Violates LaBSE's 512-token limit for long articles. Loses precision for mid-document topics. |

## Title Prepending Rationale
Title prepending was validated in the News Radar LaBSE pipeline. Articles about
the same event but with different body text were successfully clustered because
the title anchored the semantic space of every chunk. Without title prepending,
chunks from the second half of a long article often drifted semantically and
missed queries that matched the article's topic.

This mirrors the geo_district repetition technique used in News Radar — both
strategies force the embedding model to weight the most semantically significant
signal (title / location) across the full input.

## Consequences

**Makes easier:**
- Uniform chunk size produces predictable vector counts per document
- Overlap ensures query terms at chunk boundaries are captured
- Title prepending improves recall for topic-level queries without a reranker

**Makes harder:**
- More Qdrant points per document increases storage and search latency slightly
- `document_id` grouping is required in `QdrantSearchService` to avoid returning
  multiple chunks from the same article in the top-k results
- chunk_size in characters is an approximation of token count (see ADR-007)

## References
- SPEC.md §7.4 (chunking strategy)
- Issue #6 (ChunkingService implementation)
- ADR-007 (character vs token-based chunking unit)
- ADR-002 (LaBSE — the model whose token limit motivates chunking)
