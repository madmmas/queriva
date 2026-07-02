package dev.queriva.ingest;

import java.util.Optional;

import org.springframework.stereotype.Service;

/**
 * Validates and persists the embedding model associated with a Qdrant collection (ADR-002).
 */
@Service
public class CollectionEmbeddingModelService {

    private final QdrantIngestRepository qdrantIngestRepository;
    private final CollectionManager collectionManager;

    /**
     * Creates the service with Qdrant ingest and collection dependencies.
     */
    public CollectionEmbeddingModelService(
            QdrantIngestRepository qdrantIngestRepository,
            CollectionManager collectionManager) {
        this.qdrantIngestRepository = qdrantIngestRepository;
        this.collectionManager = collectionManager;
    }

    /**
     * Ensures the requested model matches the collection model, recording it on first ingest.
     */
    public void validateAndRecordModel(String collectionName, String requestedModel) {
        Optional<String> storedModel = qdrantIngestRepository.readStoredEmbeddingModel(collectionName);

        if (storedModel.isPresent()) {
            if (!storedModel.get().equals(requestedModel)) {
                throw new EmbeddingModelMismatchException(collectionName, requestedModel, storedModel.get());
            }
            return;
        }

        CollectionSummary summary = collectionManager.listCollections().collections().stream()
                .filter(collection -> collection.name().equals(collectionName))
                .findFirst()
                .orElseThrow(() -> new dev.queriva.common.CollectionNotFoundException(collectionName));

        qdrantIngestRepository.writeStoredEmbeddingModel(
                collectionName, requestedModel, summary.vectorSize());
    }

    /**
     * Validates query-time model against collection metadata without recording a new model (SPEC §8 step 2).
     */
    public void validateModelForSearch(String collectionName, String requestedModel) {
        Optional<String> storedModel = qdrantIngestRepository.readStoredEmbeddingModel(collectionName);

        if (storedModel.isPresent() && !storedModel.get().equals(requestedModel)) {
            throw new EmbeddingModelMismatchException(collectionName, requestedModel, storedModel.get());
        }
    }
}
