package dev.queriva.common;

/**
 * Raised when a Qdrant collection already exists and recreate is disabled.
 */
public class CollectionAlreadyExistsException extends RuntimeException {

    /**
     * Creates an exception describing the conflicting collection name.
     */
    public CollectionAlreadyExistsException(String collectionName) {
        super("Collection '" + collectionName + "' already exists. "
                + "Set recreate_if_exists=true to replace it, or choose a different name.");
    }
}
