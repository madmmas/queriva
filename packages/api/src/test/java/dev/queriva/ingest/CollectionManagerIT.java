package dev.queriva.ingest;

import dev.queriva.support.QdrantTestcontainersSupport;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Qdrant collection create → list → delete cycle (ADR-009).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Tag("integration")
class CollectionManagerIT {

    private static final String TEST_COLLECTION = "news_radar";
    private static final int VECTOR_SIZE = 768;
    private static final String DISTANCE = "Cosine";

    @Container
    static GenericContainer<?> qdrant = QdrantTestcontainersSupport.newQdrantContainer();

    @DynamicPropertySource
    static void configureQdrant(DynamicPropertyRegistry registry) {
        String host = qdrant.getHost();
        int restPort = qdrant.getMappedPort(QdrantTestcontainersSupport.REST_PORT);
        int grpcPort = qdrant.getMappedPort(QdrantTestcontainersSupport.GRPC_PORT);

        registry.add("qdrant.url", () -> "http://" + host + ":" + restPort);
        registry.add("qdrant.grpc-port", () -> String.valueOf(grpcPort));
    }

    @Autowired
    private CollectionManager collectionManager;

    @Test
    void should_create_list_and_delete_collection_when_qdrant_is_available() {
        CreateCollectionRequest createRequest = new CreateCollectionRequest(
                TEST_COLLECTION, VECTOR_SIZE, DISTANCE, false);

        CollectionSummary created = collectionManager.createCollection(createRequest);

        assertThat(created.name()).isEqualTo(TEST_COLLECTION);
        assertThat(created.vectorSize()).isEqualTo(VECTOR_SIZE);
        assertThat(created.distance()).isEqualTo(DISTANCE);
        assertThat(created.pointsCount()).isZero();

        CollectionListResponse listed = collectionManager.listCollections();

        assertThat(listed.collections())
                .extracting(CollectionSummary::name)
                .contains(TEST_COLLECTION);

        collectionManager.deleteCollection(TEST_COLLECTION);

        CollectionListResponse afterDelete = collectionManager.listCollections();

        assertThat(afterDelete.collections())
                .extracting(CollectionSummary::name)
                .doesNotContain(TEST_COLLECTION);
    }
}
