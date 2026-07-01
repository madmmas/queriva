package dev.queriva.ingest;

import java.util.List;

/**
 * Response body for GET /api/ingest/collections (SPEC §6).
 */
public record CollectionListResponse(List<CollectionSummary> collections) {
}
