package dev.queriva.search;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Optional Qdrant payload filters for vector search (SPEC §6).
 */
public record SearchFilters(
        String language,
        @JsonProperty("date_from") String dateFrom,
        @JsonProperty("date_to") String dateTo,
        String category
) {
}
