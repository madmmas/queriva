package dev.queriva.ingest;

/**
 * Raised when the embed sidecar fails to return a vector.
 */
public class EmbedSidecarException extends RuntimeException {

    /**
     * Creates an exception with an actionable message for the caller.
     */
    public EmbedSidecarException(String message, Throwable cause) {
        super(message, cause);
    }
}
