package dev.queriva.ingest;

/**
 * One sliding-window chunk ready for embedding and Qdrant upsert (ADR-003, ADR-007).
 */
public record Chunk(
        String documentId,
        String pointId,
        int chunkNumber,
        String embedInput,
        String bodySnippet
) {
}
