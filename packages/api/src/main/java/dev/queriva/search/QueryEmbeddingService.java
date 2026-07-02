package dev.queriva.search;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.queriva.ingest.CollectionEmbeddingModelService;
import dev.queriva.ingest.EmbedSidecarException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * HTTP client from Spring Boot to embed-sidecar for query vectorisation (SPEC §8 step 2).
 */
@Service
public class QueryEmbeddingService {

    private final RestClient embedSidecarRestClient;
    private final String embedSidecarUrl;
    private final String defaultModel;
    private final CollectionEmbeddingModelService collectionEmbeddingModelService;

    /**
     * Creates the query embedding service with configured timeouts and default model (SPEC §13).
     */
    public QueryEmbeddingService(
            RestClient.Builder restClientBuilder,
            @Value("${embed-sidecar.url}") String embedSidecarUrl,
            @Value("${embed.default-model}") String defaultModel,
            CollectionEmbeddingModelService collectionEmbeddingModelService) {
        this.embedSidecarRestClient = restClientBuilder.baseUrl(embedSidecarUrl).build();
        this.embedSidecarUrl = embedSidecarUrl;
        this.defaultModel = defaultModel;
        this.collectionEmbeddingModelService = collectionEmbeddingModelService;
    }

    /**
     * Embeds query text with the requested model, retrying once on 5xx or I/O failure.
     */
    public float[] embed(String text, String model) {
        String resolvedModel = resolveModel(model);

        for (int attempt = 0; attempt < QueryEmbeddingConstants.EMBED_MAX_ATTEMPTS; attempt++) {
            try {
                return toFloatArray(requestEmbed(text, resolvedModel).vector());
            } catch (RestClientResponseException exception) {
                boolean shouldRetry = !isClientError(exception)
                        && attempt < QueryEmbeddingConstants.EMBED_MAX_ATTEMPTS - 1;
                if (!shouldRetry) {
                    throw toEmbedSidecarException(exception, resolvedModel);
                }
            } catch (RestClientException exception) {
                if (attempt == QueryEmbeddingConstants.EMBED_MAX_ATTEMPTS - 1) {
                    throw toUnreachableException(resolvedModel, exception);
                }
            }
        }

        throw new IllegalStateException(
                "Embed retry loop exited without result for model '" + resolvedModel + "'.");
    }

    /**
     * Validates that the requested model matches the model stored in collection metadata before search.
     */
    public void validateEmbeddingModel(String collection, String model) {
        collectionEmbeddingModelService.validateModelForSearch(collection, resolveModel(model));
    }

    /**
     * Resolves a blank model to the configured default (EMBED_DEFAULT_MODEL).
     */
    public String resolveModel(String model) {
        if (model == null || model.isBlank()) {
            return defaultModel;
        }
        return model.trim();
    }

    private EmbedResponse requestEmbed(String text, String model) {
        EmbedResponse response = embedSidecarRestClient.post()
                .uri(QueryEmbeddingConstants.EMBED_API_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new EmbedRequest(text, model))
                .retrieve()
                .body(EmbedResponse.class);

        if (response == null || response.vector() == null) {
            throw new EmbedSidecarException(
                    "Embed sidecar returned an empty response for model '" + model + "'. "
                            + "Verify embed-sidecar is running: GET " + embedSidecarUrl + "/api/health",
                    null);
        }

        return response;
    }

    private static boolean isClientError(RestClientResponseException exception) {
        return exception.getStatusCode().is4xxClientError();
    }

    private static EmbedSidecarException toEmbedSidecarException(
            RestClientResponseException exception,
            String model) {
        return new EmbedSidecarException(
                "Embed sidecar returned HTTP " + exception.getStatusCode().value()
                        + " for model '" + model + "'. "
                        + "Verify embed-sidecar is running: GET /api/health on EMBED_SIDECAR_URL.",
                exception);
    }

    private static EmbedSidecarException toUnreachableException(String model, RestClientException exception) {
        return new EmbedSidecarException(
                "Embed sidecar is unreachable for model '" + model + "'. "
                        + "Verify embed-sidecar is running: GET /api/health on EMBED_SIDECAR_URL.",
                exception);
    }

    private static float[] toFloatArray(List<Float> vector) {
        float[] values = new float[vector.size()];
        for (int index = 0; index < vector.size(); index++) {
            values[index] = vector.get(index);
        }
        return values;
    }

    private record EmbedRequest(String text, String model) {
    }

    private record EmbedResponse(List<Float> vector, @JsonProperty("dimensions") int dimensions) {
    }
}
