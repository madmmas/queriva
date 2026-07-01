package dev.queriva.ingest;

/**
 * Raised when an unsupported distance metric is supplied for collection creation.
 */
public class InvalidDistanceException extends RuntimeException {

    /**
     * Creates an exception listing supported distance metrics.
     */
    public InvalidDistanceException(String distance) {
        super("Unsupported distance metric '" + distance + "'. "
                + "Use one of: Cosine, Euclid, Dot, Manhattan.");
    }
}
