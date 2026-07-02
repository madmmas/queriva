package dev.queriva.search;

/**
 * Supported search API modes (SPEC §6).
 */
public final class SearchModes {

    /** Ranked vector results only — no LLM synthesis. */
    public static final String SEARCH = "search";

    /** Vector search plus LLM synthesis (issue #17). */
    public static final String RAG = "rag";

    private SearchModes() {
    }
}
