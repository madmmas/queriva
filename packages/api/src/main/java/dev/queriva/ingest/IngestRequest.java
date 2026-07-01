package dev.queriva.ingest;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.queriva.common.ValidationPatterns;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request body for POST /api/ingest/documents (SPEC §6).
 */
public record IngestRequest(
        @NotBlank
        @Pattern(regexp = ValidationPatterns.COLLECTION_NAME)
        String collection,
        @Size(max = 64) String model,
        @NotEmpty @Valid java.util.List<IngestDocumentDto> documents,
        @Valid ChunkingOptionsDto chunking,
        @JsonProperty("upsert_mode") UpsertMode upsertMode
) {
    /**
     * Applies SPEC defaults for optional ingest request fields.
     */
    public IngestRequest {
        if (model == null || model.isBlank()) {
            model = "LaBSE";
        }
        if (chunking == null) {
            chunking = new ChunkingOptionsDto(true, 512, 64);
        }
        if (upsertMode == null) {
            upsertMode = UpsertMode.SKIP_EXISTING;
        }
    }
}
