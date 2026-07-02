package dev.queriva.search;

/**
 * Named constants for query embedding via embed-sidecar (code-quality.mdc A2).
 */
public final class QueryEmbeddingConstants {

    /** Embed sidecar POST path (SPEC §6). */
    public static final String EMBED_API_PATH = "/api/embed";

    /** Initial attempt plus one retry on 5xx or I/O failure (code-quality.mdc B5). */
    public static final int EMBED_MAX_ATTEMPTS = 2;

    /** Vector dimensions for the default LaBSE model (ADR-002). */
    public static final int LABSE_VECTOR_DIMENSIONS = 768;

    private QueryEmbeddingConstants() {
    }
}
