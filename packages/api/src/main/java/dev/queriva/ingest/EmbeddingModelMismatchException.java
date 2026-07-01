package dev.queriva.ingest;

/**
 * Raised when the requested embedding model does not match the collection model.
 */
public class EmbeddingModelMismatchException extends RuntimeException {

    /**
     * Creates an exception describing the model conflict and how to resolve it.
     */
    public EmbeddingModelMismatchException(String collection, String requestedModel, String storedModel) {
        super("Embedding model mismatch for collection '" + collection + "'. "
                + "Collection was created with model '" + storedModel + "' but request used '"
                + requestedModel + "'. Use the same model at ingest and query time.");
    }
}
