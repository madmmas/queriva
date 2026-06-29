# ADR-008 — Upsert Identity: Source document_id over Content Hash

## Status
Accepted

## Context
When ingesting documents into Qdrant, Queriva needs to determine whether a
document already exists (for `skip_existing` and `error_on_conflict` upsert
modes). Two strategies are possible:

1. **Source document_id:** the consumer provides an `id` field; Queriva uses it
   as the Qdrant point ID (or derives chunk IDs from it)
2. **Content hash:** Queriva computes a hash of the document body and uses that
   as the identity key

## Decision
Use the **source-provided `document_id`** as the identity key. The consumer is
responsible for providing a stable, unique `id` per document. Queriva derives
chunk point IDs as `{document_id}-chunk-{N}`.

## Rationale

### Why not content hash?

Content hashing breaks in the following common scenarios:

- **Article updates:** a news article is lightly edited (headline changed,
  typo fixed). Content hash changes → treated as a new document → duplicate
  in Qdrant. With source ID, the existing chunks are overwritten (if mode=overwrite)
  or skipped (if mode=skip_existing).

- **HTML scraping variability:** the same article fetched twice may have
  slightly different HTML (ads loaded, timestamp format changed) → different
  hash → duplicate.

- **News Radar integration (§7.6):** the `cluster_id` from the dedup pipeline
  is the natural and correct identity key. Computing a content hash would ignore
  the upstream dedup work already done.

### Source ID responsibility
The consumer (e.g. News Radar) is better positioned to define identity:
- News Radar uses `cluster_id` — a UUID assigned after deduplication
- A generic JSON ingest can use the document's own `id` field
- The CLI generates a UUID for documents without an explicit `id`

This makes Queriva identity-source-agnostic — it doesn't assume how consumers
define uniqueness.

## Chunk ID Derivation
For chunked documents, Qdrant point IDs are derived as:
```
{document_id}-chunk-0
{document_id}-chunk-1
{document_id}-chunk-2
...
```

On re-ingest with `overwrite`, all existing chunks for a `document_id` are
replaced. The ingest API must first delete all points matching `document_id`
before upserting new chunks.

## Consequences

**Makes easier:**
- Idempotent re-ingestion: run the same ingest twice with `skip_existing` → no duplicates
- News Radar integration is a natural fit: `cluster_id` → `document_id` → Qdrant key
- `QdrantSearchService` can group search results by `document_id` to deduplicate
  multiple-chunk hits from the same article

**Makes harder:**
- Consumers must provide stable IDs — the CLI generates random UUIDs for
  documents without `id`, which means CLI re-ingestion without `--id-field`
  will create duplicates (document in CLI README)
- Chunk deletion before overwrite adds a Qdrant API call (delete by payload filter)

## References
- SPEC.md §7.2 (ingest API fields), §7.5 (upsert modes), §14 (Qdrant payload schema)
- Issue #7 (IngestService), #8 (ingest integration tests), #12 (QdrantSearchService)
- ADR-001 (Qdrant — the store where document_id becomes the point ID)
