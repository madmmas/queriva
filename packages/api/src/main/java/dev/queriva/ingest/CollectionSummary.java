package dev.queriva.ingest;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Summary of a single Qdrant collection returned by collection management APIs (SPEC §6).
 */
public record CollectionSummary(
        String name,
        @JsonProperty("vector_size") int vectorSize,
        String distance,
        @JsonProperty("points_count") long pointsCount
) {
}
