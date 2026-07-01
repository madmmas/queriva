package dev.queriva.ingest;

/**
 * Named constants for the ingest API (code-quality.mdc A2).
 */
public final class IngestConstants {

    /** Maximum document body length accepted by POST /api/ingest/documents (code-quality.mdc E2). */
    public static final int MAX_DOCUMENT_BODY_CHARS = 100_000;

    /** Qdrant point ID storing collection-level embedding model metadata. */
    public static final String COLLECTION_METADATA_POINT_ID = "queriva-collection-metadata";

    /** Payload key for the embedding model name stored at ingest time (SPEC §6). */
    public static final String PAYLOAD_EMBEDDING_MODEL = "embedding_model";

    /** Payload marker distinguishing internal metadata points from user documents. */
    public static final String PAYLOAD_QUERIVA_INTERNAL = "queriva_internal";

    /** Payload field: document title (SPEC §14). */
    public static final String PAYLOAD_TITLE = "title";

    /** Payload field: body snippet without title prefix (SPEC §14). */
    public static final String PAYLOAD_BODY_SNIPPET = "body_snippet";

    /** Payload field: content source domain (SPEC §14). */
    public static final String PAYLOAD_SOURCE = "source";

    /** Payload field: ISO 639-1 language code (SPEC §14). */
    public static final String PAYLOAD_LANGUAGE = "language";

    /** Payload field: published timestamp (SPEC §14). */
    public static final String PAYLOAD_PUBLISHED_AT = "published_at";

    /** Payload field: content category (SPEC §14). */
    public static final String PAYLOAD_CATEGORY = "category";

    /** Payload field: canonical URL (SPEC §14). */
    public static final String PAYLOAD_URL = "url";

    /** Payload field: shared document identity (SPEC §14, ADR-008). */
    public static final String PAYLOAD_DOCUMENT_ID = "document_id";

    /** Payload field: optional dedup cluster ID (SPEC §14). */
    public static final String PAYLOAD_CLUSTER_ID = "cluster_id";

    private IngestConstants() {
    }
}
