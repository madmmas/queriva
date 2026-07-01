package dev.queriva.ingest;

import dev.queriva.common.GlobalExceptionHandler;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for collection management endpoints with mocked CollectionManager.
 */
@WebMvcTest(IngestController.class)
@Import(GlobalExceptionHandler.class)
@Tag("unit")
class IngestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CollectionManager collectionManager;

    @MockBean
    private IngestService ingestService;

    @Test
    void should_create_collection_when_request_is_valid() throws Exception {
        when(collectionManager.createCollection(any(CreateCollectionRequest.class)))
                .thenReturn(new CollectionSummary("news_radar", 768, "Cosine", 0));

        mockMvc.perform(post("/api/ingest/collection")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "collection": "news_radar",
                                  "vector_size": 768,
                                  "distance": "Cosine",
                                  "recreate_if_exists": false
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("news_radar"))
                .andExpect(jsonPath("$.vector_size").value(768))
                .andExpect(jsonPath("$.distance").value("Cosine"))
                .andExpect(jsonPath("$.points_count").value(0));
    }

    @Test
    void should_return_collections_list_with_stats() throws Exception {
        when(collectionManager.listCollections()).thenReturn(new CollectionListResponse(List.of(
                new CollectionSummary("news_radar", 768, "Cosine", 54821))));

        mockMvc.perform(get("/api/ingest/collections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collections[0].name").value("news_radar"))
                .andExpect(jsonPath("$.collections[0].vector_size").value(768))
                .andExpect(jsonPath("$.collections[0].distance").value("Cosine"))
                .andExpect(jsonPath("$.collections[0].points_count").value(54821));
    }

    @Test
    void should_return_204_when_collection_is_deleted() throws Exception {
        mockMvc.perform(delete("/api/ingest/collection/news_radar"))
                .andExpect(status().isNoContent());

        verify(collectionManager).deleteCollection("news_radar");
    }

    @Test
    void should_return_400_when_collection_name_contains_special_characters() throws Exception {
        mockMvc.perform(post("/api/ingest/collection")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "collection": "bad-name!",
                                  "vector_size": 768,
                                  "distance": "Cosine",
                                  "recreate_if_exists": false
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void should_return_400_when_collection_name_exceeds_64_characters() throws Exception {
        String longName = "a".repeat(65);

        mockMvc.perform(post("/api/ingest/collection")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "collection": "%s",
                                  "vector_size": 768,
                                  "distance": "Cosine",
                                  "recreate_if_exists": false
                                }
                                """.formatted(longName)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}
