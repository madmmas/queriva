package dev.queriva.ingest;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * One source document in POST /api/ingest/documents (SPEC §7.2, §14).
 */
public record IngestDocumentDto(
        @NotBlank String id,
        @NotBlank String title,
        @Size(max = IngestConstants.MAX_DOCUMENT_BODY_CHARS) String body,
        String source,
        String language,
        @JsonProperty("published_at") String publishedAt,
        String category,
        String url,
        @JsonProperty("cluster_id") String clusterId
) {
}
