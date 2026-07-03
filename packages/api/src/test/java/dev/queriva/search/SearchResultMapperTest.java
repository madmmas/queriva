package dev.queriva.search;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for mapping search hits to API responses.
 */
@Tag("unit")
class SearchResultMapperTest {

    @Test
    void should_map_search_mode_response_with_null_summary_and_synthesis_latency() {
        SearchResultMapper mapper = new SearchResultMapper();
        SearchRequest request = new SearchRequest(
                "floods in Dhaka",
                "news_radar",
                10,
                0.60,
                SearchModes.SEARCH,
                null);
        List<SearchHit> hits = List.of(new SearchHit(
                "cluster-001", 0.88, "Title", "Snippet", "source", "bn", "2026-06-15T08:30:00Z", "https://x"));

        SearchResponse response = mapper.toSearchResponse(request, hits, 10, 20, 35);

        assertThat(response.query()).isEqualTo("floods in Dhaka");
        assertThat(response.mode()).isEqualTo(SearchModes.SEARCH);
        assertThat(response.summary()).isNull();
        assertThat(response.results()).isEqualTo(hits);
        assertThat(response.latencyMs().embed()).isEqualTo(10);
        assertThat(response.latencyMs().search()).isEqualTo(20);
        assertThat(response.latencyMs().total()).isEqualTo(35);
        assertThat(response.latencyMs().synthesis()).isNull();
    }

    @Test
    void should_map_rag_mode_response_with_summary_and_synthesis_latency() {
        SearchResultMapper mapper = new SearchResultMapper();
        SearchRequest request = new SearchRequest(
                "floods in Dhaka",
                "news_radar",
                10,
                0.60,
                SearchModes.RAG,
                null);
        List<SearchHit> hits = List.of(new SearchHit(
                "cluster-001", 0.88, "Title", "Snippet", "source", "bn", "2026-06-15T08:30:00Z", "https://x"));

        SearchResponse response = mapper.toRagResponse(request, hits, 10, 20, 150L, "Summary text.", 185);

        assertThat(response.mode()).isEqualTo(SearchModes.RAG);
        assertThat(response.summary()).isEqualTo("Summary text.");
        assertThat(response.latencyMs().synthesis()).isEqualTo(150L);
        assertThat(response.latencyMs().total()).isEqualTo(185);
    }
}
