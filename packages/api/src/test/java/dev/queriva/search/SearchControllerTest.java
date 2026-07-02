package dev.queriva.search;

import dev.queriva.common.GlobalExceptionHandler;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WebMvc tests for POST /api/search contract and validation (issue #14).
 */
@WebMvcTest(SearchController.class)
@Import(GlobalExceptionHandler.class)
@Tag("unit")
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SearchService searchService;

    @Test
    void should_return_search_response_matching_spec_shape() throws Exception {
        SearchLatencyMs latencyMs = new SearchLatencyMs(12, 8, null, 25);
        SearchHit hit = new SearchHit(
                "cluster-001",
                0.89,
                "Buriganga river floods Dhaka low-lying areas",
                "Heavy monsoon rains caused the Buriganga river to overflow...",
                "prothomalo.com",
                "bn",
                "2026-06-15T08:30:00Z",
                "https://example.com/cluster-001");
        SearchResponse response = new SearchResponse(
                "floods in Dhaka last week",
                SearchModes.SEARCH,
                null,
                List.of(hit),
                latencyMs);

        when(searchService.search(any(SearchRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "query": "floods in Dhaka last week",
                                  "collection": "news_radar",
                                  "top_k": 10,
                                  "min_score": 0.60,
                                  "mode": "search"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("floods in Dhaka last week"))
                .andExpect(jsonPath("$.mode").value("search"))
                .andExpect(jsonPath("$.summary").doesNotExist())
                .andExpect(jsonPath("$.results[0].id").value("cluster-001"))
                .andExpect(jsonPath("$.results[0].score").value(0.89))
                .andExpect(jsonPath("$.results[0].title").exists())
                .andExpect(jsonPath("$.results[0].snippet").exists())
                .andExpect(jsonPath("$.results[0].source").value("prothomalo.com"))
                .andExpect(jsonPath("$.results[0].language").value("bn"))
                .andExpect(jsonPath("$.results[0].published_at").value("2026-06-15T08:30:00Z"))
                .andExpect(jsonPath("$.results[0].url").exists())
                .andExpect(jsonPath("$.latency_ms.embed").value(12))
                .andExpect(jsonPath("$.latency_ms.search").value(8))
                .andExpect(jsonPath("$.latency_ms.synthesis").doesNotExist())
                .andExpect(jsonPath("$.latency_ms.total").value(25));
    }

    @Test
    void should_return_400_when_query_is_blank() throws Exception {
        mockMvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "query": "",
                                  "collection": "news_radar"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void should_return_400_when_query_exceeds_1000_characters() throws Exception {
        String longQuery = "a".repeat(1001);

        mockMvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "query": "%s",
                                  "collection": "news_radar"
                                }
                                """.formatted(longQuery)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void should_return_400_when_collection_name_is_invalid() throws Exception {
        mockMvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "query": "floods in Dhaka",
                                  "collection": "invalid name!"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}
