package dev.queriva.ingest;

/**
 * Source document passed into the ingest chunking pipeline (SPEC §7.1).
 */
public record Document(
        String id,
        String title,
        String body
) {
    /**
     * Validates required identity fields used for chunk point IDs and embed input.
     */
    public Document {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(
                    "document id must not be blank. Provide a stable source document identifier.");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException(
                    "document title must not be blank. Title is prepended to every chunk embed input.");
        }
    }
}
