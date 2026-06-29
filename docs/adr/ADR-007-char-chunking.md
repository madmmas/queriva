# ADR-007 — Chunking Unit: Characters over Tokens

## Status
Accepted

## Context
The `ChunkingService` (issue #6) must split document bodies into fixed-size
units before embedding. The two main options are:

1. **Token-based:** split on LaBSE's actual tokenization (512 token limit)
2. **Character-based:** use character count as a proxy for token count

This decision affects chunking accuracy, dependency footprint, and
implementation complexity in the Java API layer.

## Decision
Use **character-based chunking** with `chunk_size = 512` characters and
`overlap = 64` characters. No tokenizer is loaded in the Java layer.

## Rationale

### Token count approximations
- English: ~4 characters per token → 512 chars ≈ 128 tokens (well within LaBSE's 512 limit)
- Bangla: ~2 characters per token → 512 chars ≈ 256 tokens (still within limit)
- Worst case: dense Bangla text at 512 chars ≈ 256 tokens — comfortably under limit

Character-based chunking will never exceed LaBSE's 512-token limit for any
realistic document language at chunk_size=512 chars. The conservative sizing
is intentional.

### Why not token-based?

Adding a tokenizer to the Java layer introduces:
- A dependency on a Python-compatible tokenizer (HuggingFace tokenizers via JNI, or a port)
- Tokenizer version pinning — must match the exact tokenizer LaBSE uses
- Different tokenizers for different models (LaBSE, MiniLM, mpnet all differ)
- Increased startup time and memory in the JVM

The embed sidecar already has the correct tokenizer — but calling the sidecar
just to tokenize (not embed) adds a network round-trip per chunk before embedding.
This would double the sidecar calls during ingest.

### Accuracy tradeoff
Character-based chunking is an approximation. For Queriva's use cases (news
articles, documents), the approximation is acceptable because:
- chunk_size=512 chars produces chunks well under the 512-token limit
- Retrieval quality is not materially different between 200-token and 256-token chunks
- The title prepending strategy (ADR-003) does more for retrieval quality than
  precise token-boundary alignment

## Consequences

**Makes easier:**
- `ChunkingService.java` has zero ML dependencies — pure string operations
- Works correctly for all three supported embedding models without model-specific logic
- Chunk sizes are predictable and human-readable in the config (`INGEST_DEFAULT_CHUNK_SIZE=512`)

**Makes harder:**
- Character count is a rough token proxy — extremely dense scripts (Thai, CJK)
  would need a lower chunk_size. Not a current use case for Queriva.
- If a future model has a much lower token limit (e.g. 128 tokens), chunk_size
  would need to be reduced. Document this in the config reference.

## References
- SPEC.md §7.4 (chunking strategy), §13 (INGEST_DEFAULT_CHUNK_SIZE config)
- Issue #6 (ChunkingService)
- ADR-003 (sliding window strategy — the what; this ADR is the why for chars)
- ADR-002 (LaBSE — the model whose 512-token limit motivates this decision)
