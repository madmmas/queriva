package dev.queriva.ingest;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response body for POST /api/ingest/documents (SPEC §6).
 */
public record IngestResponse(
        String collection,
        int ingested,
        @JsonProperty("chunks_created") int chunksCreated,
        int skipped,
        int errors,
        @JsonProperty("latency_ms") long latencyMs
) {
}
