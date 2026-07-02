package dev.queriva.search;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response body for POST /api/search (SPEC §6).
 */
public record SearchResponse(
        String query,
        String mode,
        @JsonInclude(JsonInclude.Include.NON_NULL) String summary,
        List<SearchHit> results,
        @JsonProperty("latency_ms") SearchLatencyMs latencyMs
) {
}
