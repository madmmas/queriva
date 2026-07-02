package dev.queriva.search;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One ranked search result mapped from a Qdrant point payload (SPEC §6).
 */
public record SearchHit(
        String id,
        double score,
        String title,
        String snippet,
        String source,
        String language,
        @JsonProperty("published_at") String publishedAt,
        String url
) {
}
