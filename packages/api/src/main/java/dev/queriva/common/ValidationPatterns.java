package dev.queriva.common;

/**
 * Shared validation patterns for API request fields (code-quality.mdc E2).
 */
public final class ValidationPatterns {

    /** Collection names: alphanumeric and underscore, 1–64 characters. */
    public static final String COLLECTION_NAME = "^[a-zA-Z0-9_]{1,64}$";

    private ValidationPatterns() {
    }
}
