package dev.queriva.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.queriva.common.ValidationPatterns;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request body for POST /api/search (SPEC §6).
 */
public record SearchRequest(
        @NotBlank @Size(max = 1000) String query,
        @NotBlank @Pattern(regexp = ValidationPatterns.COLLECTION_NAME) String collection,
        @JsonProperty("top_k") @Min(1) @Max(100) int topK,
        @JsonProperty("min_score") double minScore,
        String mode,
        @Valid SearchFilters filters
) {
    /**
     * Applies SPEC defaults for optional search request fields.
     */
    public SearchRequest {
        if (topK == 0) {
            topK = 10;
        }
        if (minScore == 0.0) {
            minScore = 0.60;
        }
        if (mode == null || mode.isBlank()) {
            mode = "search";
        }
        if (!SearchModes.SEARCH.equals(mode) && !SearchModes.RAG.equals(mode)) {
            throw new IllegalArgumentException(
                    "mode must be '" + SearchModes.SEARCH + "' or '" + SearchModes.RAG + "'.");
        }
    }
}
