package dev.queriva.ingest;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for collection embedding model validation at query time.
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
class CollectionEmbeddingModelServiceTest {

    private static final String COLLECTION = "news_radar";
    private static final String MODEL = "LaBSE";

    @Mock
    private QdrantIngestRepository qdrantIngestRepository;

    @Mock
    private CollectionManager collectionManager;

    private CollectionEmbeddingModelService collectionEmbeddingModelService;

    @BeforeEach
    void setUp() {
        collectionEmbeddingModelService = new CollectionEmbeddingModelService(
                qdrantIngestRepository,
                collectionManager);
    }

    @Test
    void should_pass_when_requested_model_matches_stored_model_for_search() {
        when(qdrantIngestRepository.readStoredEmbeddingModel(COLLECTION)).thenReturn(Optional.of(MODEL));

        assertThatCode(() -> collectionEmbeddingModelService.validateModelForSearch(COLLECTION, MODEL))
                .doesNotThrowAnyException();

        verify(collectionManager, never()).listCollections();
        verify(qdrantIngestRepository, never()).writeStoredEmbeddingModel(COLLECTION, MODEL, 768);
    }

    @Test
    void should_throw_when_requested_model_differs_from_stored_model_for_search() {
        when(qdrantIngestRepository.readStoredEmbeddingModel(COLLECTION)).thenReturn(Optional.of(MODEL));

        assertThatThrownBy(() -> collectionEmbeddingModelService.validateModelForSearch(
                        COLLECTION,
                        "all-MiniLM-L6-v2"))
                .isInstanceOf(EmbeddingModelMismatchException.class)
                .hasMessageContaining("Embedding model mismatch");
    }

    @Test
    void should_pass_when_collection_has_no_stored_model_yet_for_search() {
        when(qdrantIngestRepository.readStoredEmbeddingModel(COLLECTION)).thenReturn(Optional.empty());

        assertThatCode(() -> collectionEmbeddingModelService.validateModelForSearch(COLLECTION, MODEL))
                .doesNotThrowAnyException();
    }
}
