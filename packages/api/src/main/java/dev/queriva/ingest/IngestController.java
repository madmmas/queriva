package dev.queriva.ingest;

import dev.queriva.common.ValidationPatterns;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// SPEC.md §6 — collection management endpoints
@RestController
@RequestMapping("/api/ingest")
@Validated
public class IngestController {

    private final CollectionManager collectionManager;

    /**
     * Creates the ingest controller with collection lifecycle dependencies.
     */
    public IngestController(CollectionManager collectionManager) {
        this.collectionManager = collectionManager;
    }

    /**
     * Creates a Qdrant collection with the requested vector configuration.
     */
    @PostMapping("/collection")
    public ResponseEntity<CollectionSummary> createCollection(@Valid @RequestBody CreateCollectionRequest request) {
        CollectionSummary created = collectionManager.createCollection(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Lists all Qdrant collections with stats.
     */
    @GetMapping("/collections")
    public ResponseEntity<CollectionListResponse> listCollections() {
        return ResponseEntity.ok(collectionManager.listCollections());
    }

    /**
     * Deletes a Qdrant collection by name.
     */
    @DeleteMapping("/collection/{name}")
    public ResponseEntity<Void> deleteCollection(
            @PathVariable("name")
            @Pattern(regexp = ValidationPatterns.COLLECTION_NAME)
            String name) {
        collectionManager.deleteCollection(name);
        return ResponseEntity.noContent().build();
    }
}
