package dev.queriva.health;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Dependency connectivity status reported by GET /api/health (SPEC §6).
 */
public record HealthResponse(
        String status,
        String qdrant,
        String ollama,
        @JsonProperty("embed_sidecar") String embedSidecar
) {
}
