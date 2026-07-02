package dev.queriva.search;

import java.util.List;

import org.springframework.stereotype.Service;

/**
 * Maps internal search hits to the public API response shape (SPEC §6).
 */
@Service
public class SearchResultMapper {

    /**
     * Builds a search-mode response with null summary and no synthesis latency.
     */
    public SearchResponse toSearchResponse(
            SearchRequest request,
            List<SearchHit> hits,
            long embedLatencyMs,
            long searchLatencyMs,
            long totalLatencyMs) {
        return new SearchResponse(
                request.query(),
                request.mode(),
                null,
                hits,
                new SearchLatencyMs(embedLatencyMs, searchLatencyMs, null, totalLatencyMs));
    }
}
