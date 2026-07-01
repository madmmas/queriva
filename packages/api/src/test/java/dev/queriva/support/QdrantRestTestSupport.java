package dev.queriva.support;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.queriva.ingest.IngestConstants;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * Qdrant REST helpers for integration tests that verify stored point payloads.
 */
public final class QdrantRestTestSupport {

    private static final int SCROLL_LIMIT = 100;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private QdrantRestTestSupport() {
    }

    /**
     * Returns the point count reported by Qdrant for a collection.
     */
    public static long fetchPointsCount(RestTemplate restTemplate, String qdrantBaseUrl, String collectionName) {
        ResponseEntity<String> response = restTemplate.getForEntity(
                qdrantBaseUrl + "/collections/" + collectionName,
                String.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException(
                    "Qdrant collection info request failed for '" + collectionName + "': " + response.getStatusCode());
        }

        try {
            JsonNode root = OBJECT_MAPPER.readTree(response.getBody());
            return root.path("result").path("points_count").asLong();
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to parse Qdrant collection info for '" + collectionName + "'.",
                    exception);
        }
    }

    /**
     * Scrolls document chunk payloads, excluding internal Queriva metadata points.
     */
    public static List<JsonNode> scrollDocumentPayloads(
            RestTemplate restTemplate,
            String qdrantBaseUrl,
            String collectionName) {
        String scrollUrl = qdrantBaseUrl + "/collections/" + collectionName + "/points/scroll";
        String requestBody = """
                {
                  "limit": %d,
                  "with_payload": true,
                  "with_vector": false
                }
                """.formatted(SCROLL_LIMIT);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(scrollUrl, entity, String.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException(
                    "Qdrant scroll request failed for '" + collectionName + "': " + response.getStatusCode());
        }

        try {
            JsonNode points = OBJECT_MAPPER.readTree(response.getBody()).path("result").path("points");
            List<JsonNode> documentPayloads = new ArrayList<>();

            for (JsonNode point : points) {
                JsonNode payload = point.path("payload");
                if (payload.path(IngestConstants.PAYLOAD_QUERIVA_INTERNAL).asBoolean(false)) {
                    continue;
                }
                documentPayloads.add(payload);
            }

            return documentPayloads;
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to parse Qdrant scroll response for '" + collectionName + "'.",
                    exception);
        }
    }
}
