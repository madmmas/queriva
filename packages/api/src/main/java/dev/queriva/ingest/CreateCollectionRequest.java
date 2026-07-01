package dev.queriva.ingest;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.queriva.common.ValidationPatterns;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request body for POST /api/ingest/collection (SPEC §6).
 */
public record CreateCollectionRequest(
        @NotBlank
        @Pattern(regexp = ValidationPatterns.COLLECTION_NAME)
        String collection,
        @JsonProperty("vector_size")
        @Min(1) @Max(65536) int vectorSize,
        @NotBlank String distance,
        @JsonProperty("recreate_if_exists")
        boolean recreateIfExists
) {
    /**
     * Applies SPEC defaults when optional fields are omitted.
     */
    public CreateCollectionRequest {
        if (vectorSize == 0) {
            vectorSize = 768;
        }
        if (distance == null || distance.isBlank()) {
            distance = "Cosine";
        }
    }
}
