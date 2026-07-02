package dev.queriva.search;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestClient;

/**
 * Integration test against a running embed-sidecar when available (issue #13).
 */
@SpringBootTest
@Tag("integration")
class QueryEmbeddingServiceSidecarIT {

    private static final String DEFAULT_EMBED_SIDECAR_URL = "http://localhost:8001";

    @Autowired
    private QueryEmbeddingService queryEmbeddingService;

    @Test
    void should_return_768_dimensions_for_labse_when_embed_sidecar_is_running() {
        Assumptions.assumeTrue(isEmbedSidecarHealthy(), "embed-sidecar is not running at " + DEFAULT_EMBED_SIDECAR_URL);

        float[] vector = queryEmbeddingService.embed("floods in Dhaka last week", "LaBSE");

        org.assertj.core.api.Assertions.assertThat(vector)
                .hasSize(QueryEmbeddingConstants.LABSE_VECTOR_DIMENSIONS);
    }

    private static boolean isEmbedSidecarHealthy() {
        try {
            RestClient client = RestClient.builder().baseUrl(DEFAULT_EMBED_SIDECAR_URL).build();
            String body = client.get().uri("/api/health").retrieve().body(String.class);
            return body != null && body.contains("\"status\":\"ok\"");
        } catch (Exception exception) {
            return false;
        }
    }
}
