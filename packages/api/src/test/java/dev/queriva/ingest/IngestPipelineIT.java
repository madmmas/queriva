package dev.queriva.ingest;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import dev.queriva.support.NewsRadarFixtureSupport;
import dev.queriva.support.QdrantRestTestSupport;
import dev.queriva.support.QdrantTestcontainersSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full ingest pipeline integration tests using {@code fixtures/news_radar_dhaka_floods.json} (issue #8).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("integration")
class IngestPipelineIT {

    private static final String COLLECTION_CHUNKED = "news_radar_it_chunked";
    private static final String COLLECTION_UNCHUNKED = "news_radar_it_unchunked";
    private static final String MODEL = "LaBSE";
    private static final int VECTOR_SIZE = 2;
    private static final String DISTANCE = "Cosine";

    @RegisterExtension
    static WireMockExtension embedSidecar = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Container
    static GenericContainer<?> qdrant = QdrantTestcontainersSupport.newQdrantContainer();

    private static String qdrantRestBaseUrl;

    private List<IngestDocumentDto> newsRadarDocuments;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        String host = qdrant.getHost();
        int restPort = qdrant.getMappedPort(QdrantTestcontainersSupport.REST_PORT);
        int grpcPort = qdrant.getMappedPort(QdrantTestcontainersSupport.GRPC_PORT);

        qdrantRestBaseUrl = "http://" + host + ":" + restPort;

        registry.add("qdrant.url", () -> qdrantRestBaseUrl);
        registry.add("qdrant.grpc-port", () -> String.valueOf(grpcPort));
        registry.add("embed-sidecar.url", embedSidecar::baseUrl);
    }

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private CollectionManager collectionManager;

    @BeforeAll
    void loadNewsRadarFixture() {
        newsRadarDocuments = NewsRadarFixtureSupport.loadDocuments();
    }

    @BeforeEach
    void setUp() {
        embedSidecar.resetAll();
        stubEmbedResponse();
        recreateCollection(COLLECTION_CHUNKED);
        recreateCollection(COLLECTION_UNCHUNKED);
    }

    @Test
    void should_ingest_all_fixture_articles_and_show_correct_point_count_in_collections_api() {
        IngestResponse ingestResponse = ingestFixture(COLLECTION_CHUNKED, true, UpsertMode.SKIP_EXISTING);

        assertThat(ingestResponse.collection()).isEqualTo(COLLECTION_CHUNKED);
        assertThat(ingestResponse.ingested()).isEqualTo(NewsRadarFixtureSupport.DOCUMENT_COUNT);
        assertThat(ingestResponse.chunksCreated()).isEqualTo(NewsRadarFixtureSupport.CHUNKED_POINT_COUNT);
        assertThat(ingestResponse.skipped()).isZero();
        assertThat(ingestResponse.errors()).isZero();
        assertThat(ingestResponse.latencyMs()).isPositive();

        ResponseEntity<CollectionListResponse> listResponse =
                testRestTemplate.getForEntity("/api/ingest/collections", CollectionListResponse.class);

        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResponse.getBody()).isNotNull();

        CollectionSummary collectionSummary = listResponse.getBody().collections().stream()
                .filter(collection -> collection.name().equals(COLLECTION_CHUNKED))
                .findFirst()
                .orElseThrow();

        long expectedPoints = NewsRadarFixtureSupport.CHUNKED_POINT_COUNT + NewsRadarFixtureSupport.METADATA_POINT_COUNT;
        assertThat(collectionSummary.pointsCount()).isEqualTo(expectedPoints);
        assertThat(QdrantRestTestSupport.fetchPointsCount(restClient(), qdrantRestBaseUrl, COLLECTION_CHUNKED))
                .isEqualTo(expectedPoints);
    }

    @Test
    void should_create_more_points_with_chunking_enabled_than_when_chunking_is_disabled() {
        IngestResponse chunkedResponse = ingestFixture(COLLECTION_CHUNKED, true, UpsertMode.SKIP_EXISTING);
        IngestResponse unchunkedResponse = ingestFixture(COLLECTION_UNCHUNKED, false, UpsertMode.SKIP_EXISTING);

        assertThat(chunkedResponse.chunksCreated()).isGreaterThan(unchunkedResponse.chunksCreated());
        assertThat(chunkedResponse.chunksCreated()).isEqualTo(NewsRadarFixtureSupport.CHUNKED_POINT_COUNT);
        assertThat(unchunkedResponse.chunksCreated()).isEqualTo(NewsRadarFixtureSupport.UNCHUNKED_POINT_COUNT);

        long chunkedTotal = NewsRadarFixtureSupport.CHUNKED_POINT_COUNT + NewsRadarFixtureSupport.METADATA_POINT_COUNT;
        long unchunkedTotal = NewsRadarFixtureSupport.UNCHUNKED_POINT_COUNT + NewsRadarFixtureSupport.METADATA_POINT_COUNT;

        assertThat(QdrantRestTestSupport.fetchPointsCount(restClient(), qdrantRestBaseUrl, COLLECTION_CHUNKED))
                .isEqualTo(chunkedTotal);
        assertThat(QdrantRestTestSupport.fetchPointsCount(restClient(), qdrantRestBaseUrl, COLLECTION_UNCHUNKED))
                .isEqualTo(unchunkedTotal);
    }

    @Test
    void should_store_news_radar_payload_fields_with_document_id_matching_fixture_id() {
        ingestFixture(COLLECTION_CHUNKED, true, UpsertMode.SKIP_EXISTING);

        List<JsonNode> payloads = QdrantRestTestSupport.scrollDocumentPayloads(
                restClient(), qdrantRestBaseUrl, COLLECTION_CHUNKED);

        assertThat(payloads).hasSize(NewsRadarFixtureSupport.CHUNKED_POINT_COUNT);

        Set<String> fixtureDocumentIds = newsRadarDocuments.stream()
                .map(IngestDocumentDto::id)
                .collect(Collectors.toSet());

        for (JsonNode payload : payloads) {
            assertThat(payload.has(IngestConstants.PAYLOAD_TITLE)).isTrue();
            assertThat(payload.has(IngestConstants.PAYLOAD_BODY_SNIPPET)).isTrue();
            assertThat(payload.has(IngestConstants.PAYLOAD_SOURCE)).isTrue();
            assertThat(payload.has(IngestConstants.PAYLOAD_LANGUAGE)).isTrue();
            assertThat(payload.has(IngestConstants.PAYLOAD_PUBLISHED_AT)).isTrue();
            assertThat(payload.has(IngestConstants.PAYLOAD_CATEGORY)).isTrue();
            assertThat(payload.has(IngestConstants.PAYLOAD_URL)).isTrue();
            assertThat(payload.has(IngestConstants.PAYLOAD_DOCUMENT_ID)).isTrue();

            String documentId = payload.path(IngestConstants.PAYLOAD_DOCUMENT_ID).asText();
            assertThat(fixtureDocumentIds).contains(documentId);
        }

        assertThat(payloads.stream()
                .map(payload -> payload.path(IngestConstants.PAYLOAD_DOCUMENT_ID).asText())
                .collect(Collectors.toSet()))
                .containsExactlyInAnyOrderElementsOf(fixtureDocumentIds);
    }

    @Test
    void should_ingest_bangla_and_english_articles_from_fixture() {
        ingestFixture(COLLECTION_CHUNKED, true, UpsertMode.SKIP_EXISTING);

        List<JsonNode> payloads = QdrantRestTestSupport.scrollDocumentPayloads(
                restClient(), qdrantRestBaseUrl, COLLECTION_CHUNKED);

        long banglaChunks = payloads.stream()
                .filter(payload -> "bn".equals(payload.path(IngestConstants.PAYLOAD_LANGUAGE).asText()))
                .count();
        long englishChunks = payloads.stream()
                .filter(payload -> "en".equals(payload.path(IngestConstants.PAYLOAD_LANGUAGE).asText()))
                .count();

        assertThat(banglaChunks).isPositive();
        assertThat(englishChunks).isPositive();

        long banglaDocuments = newsRadarDocuments.stream()
                .filter(document -> "bn".equals(document.language()))
                .count();
        long englishDocuments = newsRadarDocuments.stream()
                .filter(document -> "en".equals(document.language()))
                .count();

        assertThat(banglaDocuments).isEqualTo(NewsRadarFixtureSupport.BANGLA_DOCUMENT_COUNT);
        assertThat(englishDocuments).isEqualTo(NewsRadarFixtureSupport.ENGLISH_DOCUMENT_COUNT);
    }

    @Test
    void should_skip_all_fixture_documents_on_idempotent_reingest_with_skip_existing() {
        IngestResponse firstRun = ingestFixture(COLLECTION_CHUNKED, true, UpsertMode.SKIP_EXISTING);
        IngestResponse secondRun = ingestFixture(COLLECTION_CHUNKED, true, UpsertMode.SKIP_EXISTING);

        assertThat(firstRun.ingested()).isEqualTo(NewsRadarFixtureSupport.DOCUMENT_COUNT);
        assertThat(secondRun.ingested()).isZero();
        assertThat(secondRun.skipped()).isEqualTo(NewsRadarFixtureSupport.DOCUMENT_COUNT);
        assertThat(secondRun.chunksCreated()).isZero();

        long expectedPoints = NewsRadarFixtureSupport.CHUNKED_POINT_COUNT + NewsRadarFixtureSupport.METADATA_POINT_COUNT;
        assertThat(QdrantRestTestSupport.fetchPointsCount(restClient(), qdrantRestBaseUrl, COLLECTION_CHUNKED))
                .isEqualTo(expectedPoints);
    }

    @Test
    void should_complete_fixture_ingest_within_thirty_second_baseline() {
        IngestResponse ingestResponse = ingestFixture(COLLECTION_CHUNKED, true, UpsertMode.SKIP_EXISTING);

        assertThat(ingestResponse.latencyMs()).isLessThan(NewsRadarFixtureSupport.INGEST_BASELINE_MILLIS);
    }

    private IngestResponse ingestFixture(String collectionName, boolean chunkingEnabled, UpsertMode upsertMode) {
        IngestRequest request = new IngestRequest(
                collectionName,
                MODEL,
                newsRadarDocuments,
                new ChunkingOptionsDto(chunkingEnabled, 512, 64),
                upsertMode);

        ResponseEntity<IngestResponse> response =
                testRestTemplate.postForEntity("/api/ingest/documents", request, IngestResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        return response.getBody();
    }

    private void recreateCollection(String collectionName) {
        try {
            collectionManager.deleteCollection(collectionName);
        } catch (Exception ignored) {
            // collection may not exist on first run
        }

        collectionManager.createCollection(
                new CreateCollectionRequest(collectionName, VECTOR_SIZE, DISTANCE, true));
    }

    private RestTemplate restClient() {
        return testRestTemplate.getRestTemplate();
    }

    private static void stubEmbedResponse() {
        embedSidecar.stubFor(post(urlEqualTo("/api/embed"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"vector\":[0.1,0.2],\"dimensions\":2}")));
    }
}
