package dev.queriva.health;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.qdrant.client.QdrantClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Actively probes Qdrant, Ollama, and embed-sidecar for GET /api/health (code-quality.mdc F4).
 */
@Service
public class DependencyHealthService {

    private static final Logger logger = LoggerFactory.getLogger(DependencyHealthService.class);
    private static final Duration QDRANT_PROBE_TIMEOUT = Duration.ofSeconds(5);
    private static final String STATUS_OK = "ok";
    private static final String STATUS_DEGRADED = "degraded";
    private static final String OLLAMA_TAGS_PATH = "/api/tags";
    private static final String EMBED_HEALTH_PATH = "/api/health";

    private final QdrantClient qdrantClient;
    private final RestClient ollamaRestClient;
    private final RestClient embedSidecarRestClient;

    /**
     * Creates the health service with configured dependency clients.
     */
    public DependencyHealthService(
            QdrantClient qdrantClient,
            RestClient.Builder restClientBuilder,
            @Value("${ollama.url}") String ollamaUrl,
            @Value("${embed-sidecar.url}") String embedSidecarUrl) {
        this.qdrantClient = qdrantClient;
        this.ollamaRestClient = restClientBuilder.baseUrl(ollamaUrl).build();
        this.embedSidecarRestClient = restClientBuilder.baseUrl(embedSidecarUrl).build();
    }

    /**
     * Probes all dependencies and returns aggregate health per SPEC §6.
     */
    public HealthResponse checkHealth() {
        DependencyStatus qdrantStatus = probeQdrant();
        DependencyStatus ollamaStatus = probeHttpDependency(ollamaRestClient, OLLAMA_TAGS_PATH, "Ollama");
        DependencyStatus embedStatus = probeHttpDependency(
                embedSidecarRestClient, EMBED_HEALTH_PATH, "embed-sidecar");

        String overallStatus = resolveOverallStatus(qdrantStatus, embedStatus);

        return new HealthResponse(
                overallStatus,
                qdrantStatus.label(),
                ollamaStatus.label(),
                embedStatus.label());
    }

    private String resolveOverallStatus(DependencyStatus qdrantStatus, DependencyStatus embedStatus) {
        if (qdrantStatus == DependencyStatus.CONNECTED && embedStatus == DependencyStatus.CONNECTED) {
            return STATUS_OK;
        }
        return STATUS_DEGRADED;
    }

    private DependencyStatus probeQdrant() {
        try {
            qdrantClient.healthCheckAsync(QDRANT_PROBE_TIMEOUT)
                    .get(QDRANT_PROBE_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            return DependencyStatus.CONNECTED;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            logger.warn("Qdrant health probe interrupted");
            return DependencyStatus.DISCONNECTED;
        } catch (ExecutionException | TimeoutException exception) {
            logger.warn("Qdrant health probe failed: {}", exception.getMessage());
            return DependencyStatus.DISCONNECTED;
        }
    }

    private DependencyStatus probeHttpDependency(RestClient client, String path, String dependencyName) {
        try {
            client.get()
                    .uri(path)
                    .retrieve()
                    .toBodilessEntity();
            return DependencyStatus.CONNECTED;
        } catch (RestClientException exception) {
            logger.warn("{} health probe failed: {}", dependencyName, exception.getMessage());
            return DependencyStatus.DISCONNECTED;
        }
    }
}
