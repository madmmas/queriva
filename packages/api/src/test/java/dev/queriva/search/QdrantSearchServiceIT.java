package dev.queriva.search;

import java.util.List;
import java.util.stream.Stream;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import dev.queriva.ingest.ChunkingOptionsDto;
import dev.queriva.ingest.CollectionManager;
import dev.queriva.ingest.CreateCollectionRequest;
import dev.queriva.ingest.IngestDocumentDto;
import dev.queriva.ingest.IngestRequest;
import dev.queriva.ingest.IngestResponse;
import dev.queriva.ingest.UpsertMode;
import dev.queriva.support.NewsRadarFixtureSupport;
import dev.queriva.support.QdrantTestcontainersSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
 * Integration tests for QdrantSearchService against seeded fixture data (issue #12).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("integration")
class QdrantSearchServiceIT {

    private static final String COLLECTION = "news_radar_search_it";
    private static final String MODEL = "LaBSE";
    private static final int VECTOR_SIZE = 2;
    private static final String DISTANCE = "Cosine";
    private static final float[] QUERY_VECTOR = new float[] {0.1f, 0.2f};
    private static final float[] ORTHOGONAL_QUERY_VECTOR = new float[] {1.0f, 0.0f};

    @RegisterExtension
    static WireMockExtension embedSidecar = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Container
    static GenericContainer<?> qdrant = QdrantTestcontainersSupport.newQdrantContainer();

    private List<IngestDocumentDto> newsRadarDocuments;

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
    private TestRestTemplate testRestTemplate;

    @Autowired
    private CollectionManager collectionManager;

    @Autowired
    private QdrantSearchService qdrantSearchService;

    @BeforeAll
    void loadNewsRadarFixture() {
        newsRadarDocuments = NewsRadarFixtureSupport.loadDocuments();
    }

    @BeforeEach
    void setUp() {
        embedSidecar.resetAll();
        stubEmbedResponse();
        recreateCollection();
        ingestFixture();
    }

    @Test
    void should_return_ranked_results_against_seeded_news_radar_collection() {
        List<SearchHit> hits = qdrantSearchService.search(COLLECTION, QUERY_VECTOR, 10, 0.60, null);

        assertThat(hits).isNotEmpty();
        assertThat(hits.getFirst().score()).isGreaterThanOrEqualTo(0.60);
        assertThat(hits.getFirst().title()).isNotBlank();
        assertThat(hits.getFirst().snippet()).isNotBlank();
    }

    @Test
    void should_return_only_bangla_articles_when_language_filter_is_bn() {
        SearchFilters filters = new SearchFilters("bn", null, null, null);

        List<SearchHit> hits = qdrantSearchService.search(COLLECTION, QUERY_VECTOR, 10, 0.60, filters);

        assertThat(hits).isNotEmpty();
        assertThat(hits).extracting(SearchHit::language).containsOnly("bn");
    }

    @Test
    void should_filter_results_by_published_at_date_range() {
        SearchFilters filters = new SearchFilters(null, "2026-06-15", "2026-06-15", null);

        List<SearchHit> hits = qdrantSearchService.search(COLLECTION, QUERY_VECTOR, 10, 0.60, filters);

        assertThat(hits).extracting(SearchHit::id).contains("cluster-001");
        assertThat(hits).extracting(SearchHit::publishedAt).allMatch("2026-06-15T08:30:00Z"::equals);
    }

    @Test
    void should_return_empty_results_when_min_score_is_very_high() {
        List<SearchHit> hits = qdrantSearchService.search(
                COLLECTION,
                ORTHOGONAL_QUERY_VECTOR,
                10,
                0.99,
                null);

        assertThat(hits).isEmpty();
    }

    @Test
    void should_return_only_one_result_per_document_id_in_top_k() {
        List<SearchHit> hits = qdrantSearchService.search(COLLECTION, QUERY_VECTOR, 10, 0.60, null);

        assertThat(hits).extracting(SearchHit::id).doesNotHaveDuplicates();
        assertThat(hits.size()).isLessThanOrEqualTo(NewsRadarFixtureSupport.DOCUMENT_COUNT);
    }

    @ParameterizedTest
    @MethodSource("filterCombinations")
    void should_filter_results_correctly(SearchFilters filters, int minimumExpectedCount) {
        List<SearchHit> hits = qdrantSearchService.search(COLLECTION, QUERY_VECTOR, 10, 0.60, filters);

        assertThat(hits).hasSizeGreaterThanOrEqualTo(minimumExpectedCount);

        if (filters.language() != null) {
            assertThat(hits).extracting(SearchHit::language).containsOnly(filters.language());
        }

        if (filters.category() != null) {
            assertThat(hits).extracting(SearchHit::id).isNotEmpty();
        }
    }

    static Stream<Arguments> filterCombinations() {
        return Stream.of(
                Arguments.of(new SearchFilters("bn", null, null, null), 1),
                Arguments.of(new SearchFilters("en", null, null, null), 1),
                Arguments.of(new SearchFilters(null, "2026-06-15", "2026-06-17", null), 1),
                Arguments.of(new SearchFilters(null, null, null, "national"), 1));
    }

    private void ingestFixture() {
        IngestRequest request = new IngestRequest(
                COLLECTION,
                MODEL,
                newsRadarDocuments,
                new ChunkingOptionsDto(true, 512, 64),
                UpsertMode.SKIP_EXISTING);

        ResponseEntity<IngestResponse> response =
                testRestTemplate.postForEntity("/api/ingest/documents", request, IngestResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().ingested()).isEqualTo(NewsRadarFixtureSupport.DOCUMENT_COUNT);
    }

    private void recreateCollection() {
        try {
            collectionManager.deleteCollection(COLLECTION);
        } catch (Exception ignored) {
            // collection may not exist on first run
        }

        collectionManager.createCollection(
                new CreateCollectionRequest(COLLECTION, VECTOR_SIZE, DISTANCE, true));
    }

    private static void stubEmbedResponse() {
        embedSidecar.stubFor(post(urlEqualTo("/api/embed"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"vector\":[0.1,0.2],\"dimensions\":2}")));
    }
}
