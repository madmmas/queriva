package dev.queriva.ingest;

import java.util.List;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import dev.queriva.support.QdrantTestcontainersSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for ingest orchestration against Testcontainers Qdrant.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Tag("integration")
class IngestServiceIT {

    private static final String COLLECTION = "ingest_it";

    @RegisterExtension
    static WireMockExtension embedSidecar = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Container
    static GenericContainer<?> qdrant = QdrantTestcontainersSupport.newQdrantContainer();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        String host = qdrant.getHost();
        int restPort = qdrant.getMappedPort(QdrantTestcontainersSupport.REST_PORT);
        int grpcPort = qdrant.getMappedPort(QdrantTestcontainersSupport.GRPC_PORT);

        registry.add("qdrant.url", () -> "http://" + host + ":" + restPort);
        registry.add("qdrant.grpc-port", () -> String.valueOf(grpcPort));
        registry.add("embed-sidecar.url", embedSidecar::baseUrl);
    }

    @Autowired
    private CollectionManager collectionManager;

    @Autowired
    private IngestService ingestService;

    @BeforeEach
    void setUp() {
        embedSidecar.resetAll();
        stubEmbedResponse();

        try {
            collectionManager.deleteCollection(COLLECTION);
        } catch (Exception ignored) {
            // collection may not exist on first run
        }

        collectionManager.createCollection(new CreateCollectionRequest(COLLECTION, 2, "Cosine", true));
    }

    @Test
    void should_ingest_three_documents_and_skip_existing_on_rerun() {
        List<IngestDocumentDto> documents = List.of(
                document("doc-1", "First"),
                document("doc-2", "Second"),
                document("doc-3", "Third"));
        IngestRequest request = new IngestRequest(
                COLLECTION,
                "LaBSE",
                documents,
                new ChunkingOptionsDto(true, 512, 64),
                UpsertMode.SKIP_EXISTING);

        IngestResponse firstRun = ingestService.ingestDocuments(request);

        assertThat(firstRun.ingested()).isEqualTo(3);
        assertThat(firstRun.chunksCreated()).isEqualTo(3);
        assertThat(firstRun.skipped()).isZero();

        IngestResponse secondRun = ingestService.ingestDocuments(request);

        assertThat(secondRun.ingested()).isZero();
        assertThat(secondRun.skipped()).isEqualTo(3);
    }

    @Test
    void should_replace_chunks_when_overwrite_mode_is_used() {
        IngestDocumentDto document = document("doc-overwrite", "Overwrite me");
        IngestRequest request = new IngestRequest(
                COLLECTION,
                "LaBSE",
                List.of(document),
                new ChunkingOptionsDto(false, 512, 64),
                UpsertMode.OVERWRITE);

        IngestResponse firstRun = ingestService.ingestDocuments(request);
        IngestResponse secondRun = ingestService.ingestDocuments(request);

        assertThat(firstRun.ingested()).isEqualTo(1);
        assertThat(secondRun.ingested()).isEqualTo(1);
        assertThat(secondRun.skipped()).isZero();
    }

    private static IngestDocumentDto document(String id, String title) {
        return new IngestDocumentDto(
                id,
                title,
                "Flood update body for " + title,
                "prothomalo.com",
                "en",
                "2026-06-15T08:30:00Z",
                "national",
                "https://example.com/" + id,
                id);
    }

    private static void stubEmbedResponse() {
        embedSidecar.stubFor(post(urlEqualTo("/api/embed"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"vector\":[0.1,0.2],\"dimensions\":2}")));
    }
}
