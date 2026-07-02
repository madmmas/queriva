package dev.queriva.search;

import java.time.Duration;

/**
 * Named constants for vector search (code-quality.mdc A2).
 */
public final class SearchConstants {

    /** Multiplier applied to top_k when over-fetching chunk points before document deduplication. */
    public static final int SEARCH_OVERFETCH_MULTIPLIER = 5;

    /** Qdrant search operation timeout (SPEC §13). */
    public static final Duration SEARCH_OPERATION_TIMEOUT = Duration.ofSeconds(30);

    /** Suffix appended to date-only filter values for inclusive start-of-day bounds. */
    public static final String DATE_FILTER_START_SUFFIX = "T00:00:00Z";

    /** Suffix appended to date-only filter values for inclusive end-of-day bounds. */
    public static final String DATE_FILTER_END_SUFFIX = "T23:59:59Z";

    private SearchConstants() {
    }
}
