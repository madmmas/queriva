package dev.queriva.ingest;

import dev.queriva.common.GlobalExceptionHandler;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WebMvc tests for POST /api/ingest/documents contract and validation.
 */
@WebMvcTest(IngestController.class)
@Import(GlobalExceptionHandler.class)
@Tag("unit")
class IngestDocumentsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CollectionManager collectionManager;

    @MockBean
    private IngestService ingestService;

    @Test
    void should_return_ingest_response_matching_spec_shape() throws Exception {
        when(ingestService.ingestDocuments(any(IngestRequest.class)))
                .thenReturn(new IngestResponse("news_radar", 1, 2, 0, 0, 120));

        mockMvc.perform(post("/api/ingest/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "collection": "news_radar",
                                  "model": "LaBSE",
                                  "documents": [{
                                    "id": "cluster-001",
                                    "title": "Floods in Dhaka",
                                    "body": "Heavy rains caused flooding.",
                                    "source": "prothomalo.com",
                                    "language": "bn"
                                  }],
                                  "chunking": { "enabled": true, "chunk_size": 512, "overlap": 64 },
                                  "upsert_mode": "skip_existing"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection").value("news_radar"))
                .andExpect(jsonPath("$.ingested").value(1))
                .andExpect(jsonPath("$.chunks_created").value(2))
                .andExpect(jsonPath("$.skipped").value(0))
                .andExpect(jsonPath("$.errors").value(0))
                .andExpect(jsonPath("$.latency_ms").value(120));
    }

    @Test
    void should_return_400_when_document_body_exceeds_100000_characters() throws Exception {
        String longBody = "a".repeat(100_001);

        mockMvc.perform(post("/api/ingest/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "collection": "news_radar",
                                  "documents": [{
                                    "id": "cluster-001",
                                    "title": "Floods in Dhaka",
                                    "body": "%s"
                                  }]
                                }
                                """.formatted(longBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void should_return_409_when_ingest_service_raises_document_conflict() throws Exception {
        when(ingestService.ingestDocuments(any(IngestRequest.class)))
                .thenThrow(new DocumentConflictException("cluster-001"));

        mockMvc.perform(post("/api/ingest/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "collection": "news_radar",
                                  "documents": [{
                                    "id": "cluster-001",
                                    "title": "Floods in Dhaka",
                                    "body": "Heavy rains caused flooding."
                                  }],
                                  "upsert_mode": "error_on_conflict"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());
    }
}
