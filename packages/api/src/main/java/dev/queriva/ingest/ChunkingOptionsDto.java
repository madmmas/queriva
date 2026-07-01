package dev.queriva.ingest;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Chunking configuration for POST /api/ingest/documents (SPEC §7.2).
 */
public record ChunkingOptionsDto(
        boolean enabled,
        @JsonProperty("chunk_size") @Min(1) @Max(65536) int chunkSize,
        @Min(0) @Max(65535) int overlap
) {
    /**
     * Applies SPEC defaults when optional chunking fields are omitted.
     */
    public ChunkingOptionsDto {
        if (chunkSize == 0) {
            chunkSize = 512;
        }
    }
}
