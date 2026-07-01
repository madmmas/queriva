package dev.queriva.common;

/**
 * Raised when a requested Qdrant collection does not exist.
 */
public class CollectionNotFoundException extends RuntimeException {

    /**
     * Creates an exception describing the missing collection name.
     */
    public CollectionNotFoundException(String collectionName) {
        super("Collection '" + collectionName + "' was not found. "
                + "Create it first with POST /api/ingest/collection.");
    }
}
