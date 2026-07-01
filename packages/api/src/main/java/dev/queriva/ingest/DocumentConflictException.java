package dev.queriva.ingest;

/**
 * Raised when upsert_mode=error_on_conflict and a document already exists in Qdrant.
 */
public class DocumentConflictException extends RuntimeException {

    /**
     * Creates an exception describing the conflicting document id.
     */
    public DocumentConflictException(String documentId) {
        super("Document '" + documentId + "' already exists in the collection. "
                + "Use upsert_mode=overwrite to replace chunks or skip_existing to ignore duplicates.");
    }
}
