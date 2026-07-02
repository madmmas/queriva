package dev.queriva.search;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Latency breakdown for POST /api/search (SPEC §6).
 */
public record SearchLatencyMs(
        long embed,
        long search,
        @JsonInclude(JsonInclude.Include.NON_NULL) Long synthesis,
        long total
) {
}
