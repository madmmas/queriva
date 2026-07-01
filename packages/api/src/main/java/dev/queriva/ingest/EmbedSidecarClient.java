package dev.queriva.ingest;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * HTTP client for the embed sidecar POST /api/embed endpoint (SPEC §6).
 */
@Component
public class EmbedSidecarClient {

    private static final String EMBED_PATH = "/api/embed";

    private final RestClient embedSidecarRestClient;

    /**
     * Creates the embed sidecar client with configured timeouts (code-quality.mdc B5).
     */
    public EmbedSidecarClient(
            RestClient.Builder restClientBuilder,
            @Value("${embed-sidecar.url}") String embedSidecarUrl) {
        this.embedSidecarRestClient = restClientBuilder.baseUrl(embedSidecarUrl).build();
    }

    /**
     * Embeds a single text string with the requested model.
     */
    public List<Float> embed(String text, String model) {
        try {
            EmbedResponse response = embedSidecarRestClient.post()
                    .uri(EMBED_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new EmbedRequest(text, model))
                    .retrieve()
                    .body(EmbedResponse.class);

            if (response == null || response.vector() == null) {
                throw new EmbedSidecarException(
                        "Embed sidecar returned an empty response for model '" + model + "'. "
                                + "Verify embed-sidecar is running at EMBED_SIDECAR_URL.",
                        null);
            }

            return response.vector();
        } catch (RestClientResponseException exception) {
            throw new EmbedSidecarException(
                    "Embed sidecar returned HTTP " + exception.getStatusCode().value()
                            + " for model '" + model + "'. Verify embed-sidecar is running at EMBED_SIDECAR_URL.",
                    exception);
        } catch (RestClientException exception) {
            throw new EmbedSidecarException(
                    "Embed sidecar is unreachable for model '" + model + "'. "
                            + "Verify embed-sidecar is running at EMBED_SIDECAR_URL.",
                    exception);
        }
    }

    private record EmbedRequest(String text, String model) {
    }

    private record EmbedResponse(List<Float> vector, @JsonProperty("dimensions") int dimensions) {
    }
}
