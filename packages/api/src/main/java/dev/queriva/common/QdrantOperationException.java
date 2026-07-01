package dev.queriva.common;

/**
 * Raised when a Qdrant operation fails after retries.
 */
public class QdrantOperationException extends RuntimeException {

    /**
     * Creates an exception with an actionable message for the caller.
     */
    public QdrantOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
