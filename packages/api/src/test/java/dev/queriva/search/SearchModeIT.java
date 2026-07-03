package dev.queriva.search;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import dev.queriva.support.SearchIntegrationTestSupport;
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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static dev.queriva.support.SearchIntegrationTestSupport.DHAKA_FLOODS_QUERY;
import static dev.queriva.support.SearchIntegrationTestSupport.FIRST_ARTICLE_ID;
import static dev.queriva.support.SearchIntegrationTestSupport.FIRST_ARTICLE_TITLE;
import static dev.queriva.support.SearchIntegrationTestSupport.SEARCH_MODE_BASELINE_MILLIS;
import static org.assertj.core.api.Assertions.assertThat;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/**
 * Full search-mode integration coverage using the News Radar fixture corpus (issue #15).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("integration")
class SearchModeIT {

    private static final String COLLECTION = "news_radar_search_mode_it";
    private static final String MODEL = "LaBSE";
    private static final String DISTANCE = "Cosine";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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

    @BeforeAll
    void loadNewsRadarFixture() {
        newsRadarDocuments = NewsRadarFixtureSupport.loadDocuments();
        assertThat(newsRadarDocuments.getFirst().id()).isEqualTo(FIRST_ARTICLE_ID);
        assertThat(newsRadarDocuments.getFirst().title()).isEqualTo(FIRST_ARTICLE_TITLE);
    }

    @BeforeEach
    void setUp() {
        prepareCollection(SearchIntegrationTestSupport::stubRankedEmbedResponses);
    }

    @Test
    void should_return_first_fixture_article_as_top_result_for_dhaka_floods_query() {
        SearchResponse response = search(defaultRequest(10, 0.60, null));

        assertThat(response.results()).isNotEmpty();
        assertThat(response.results().getFirst().id()).isEqualTo(FIRST_ARTICLE_ID);
        assertThat(response.results().getFirst().title()).isEqualTo(FIRST_ARTICLE_TITLE);
    }

    @Test
    void should_return_exactly_three_results_when_top_k_is_three() {
        prepareCollection(SearchIntegrationTestSupport::stubUniformEmbedResponses);

        SearchResponse response = search(defaultRequest(3, 0.60, null));

        assertThat(response.results()).hasSize(3);
    }

    @Test
    void should_return_zero_results_when_min_score_is_0_99() {
        prepareCollection(SearchIntegrationTestSupport::stubLowSimilarityEmbedResponses);

        SearchResponse response = search(defaultRequest(10, 0.99, null));

        assertThat(response.results()).isEmpty();
    }

    @Test
    void should_return_only_bangla_articles_when_language_filter_is_bn() {
        prepareCollection(SearchIntegrationTestSupport::stubUniformEmbedResponses);

        SearchFilters filters = new SearchFilters("bn", null, null, null);
        SearchResponse response = search(defaultRequest(10, 0.60, filters));

        assertThat(response.results()).isNotEmpty();
        assertThat(response.results()).extracting(SearchHit::language).containsOnly("bn");
        assertThat(response.results().size()).isLessThanOrEqualTo(NewsRadarFixtureSupport.BANGLA_DOCUMENT_COUNT);
    }

    @Test
    void should_return_only_english_articles_when_language_filter_is_en() {
        prepareCollection(SearchIntegrationTestSupport::stubUniformEmbedResponses);

        SearchFilters filters = new SearchFilters("en", null, null, null);
        SearchResponse response = search(defaultRequest(10, 0.60, filters));

        assertThat(response.results()).isNotEmpty();
        assertThat(response.results()).extracting(SearchHit::language).containsOnly("en");
        assertThat(response.results().size()).isLessThanOrEqualTo(NewsRadarFixtureSupport.ENGLISH_DOCUMENT_COUNT);
    }

    @Test
    void should_narrow_results_when_date_range_filter_is_applied() {
        prepareCollection(SearchIntegrationTestSupport::stubUniformEmbedResponses);

        SearchResponse unfiltered = search(defaultRequest(10, 0.60, null));
        SearchFilters dateFilter = new SearchFilters(null, "2026-06-15", "2026-06-15", null);
        SearchResponse filtered = search(defaultRequest(10, 0.60, dateFilter));

        assertThat(filtered.results()).isNotEmpty();
        assertThat(filtered.results().size()).isLessThan(unfiltered.results().size());
        assertThat(filtered.results()).extracting(SearchHit::id).contains(FIRST_ARTICLE_ID);
        assertThat(filtered.results()).extracting(SearchHit::publishedAt)
                .allMatch("2026-06-15T08:30:00Z"::equals);
    }

    @Test
    void should_complete_search_mode_within_performance_baseline() {
        SearchResponse response = search(defaultRequest(10, 0.60, null));

        assertThat(response.latencyMs().total()).isLessThan(SEARCH_MODE_BASELINE_MILLIS);
        assertThat(response.latencyMs().embed()).isPositive();
        assertThat(response.latencyMs().search()).isPositive();
    }

    @Test
    void should_return_complete_search_response_shape_matching_spec_section_six() throws Exception {
        SearchResponse response = search(defaultRequest(10, 0.60, null));

        assertCompleteSearchResponseShape(response);

        ResponseEntity<String> rawResponse = testRestTemplate.postForEntity(
                "/api/search",
                defaultRequest(10, 0.60, null),
                String.class);

        JsonNode body = OBJECT_MAPPER.readTree(rawResponse.getBody());
        assertThat(body.get("summary").isNull()).isTrue();
        assertThat(body.get("latency_ms").has("synthesis")).isFalse();
    }

    private SearchRequest defaultRequest(int topK, double minScore, SearchFilters filters) {
        return new SearchRequest(
                DHAKA_FLOODS_QUERY,
                COLLECTION,
                topK,
                minScore,
                SearchModes.SEARCH,
                filters);
    }

    private SearchResponse search(SearchRequest request) {
        ResponseEntity<SearchResponse> response = testRestTemplate.postForEntity(
                "/api/search",
                request,
                SearchResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
    }

    private void prepareCollection(EmbedStubConfigurer embedStubConfigurer) {
        embedSidecar.resetAll();
        embedStubConfigurer.configure(embedSidecar);
        recreateCollection();
        ingestFixture();
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

        collectionManager.createCollection(new CreateCollectionRequest(
                COLLECTION,
                SearchIntegrationTestSupport.INTEGRATION_VECTOR_SIZE,
                DISTANCE,
                true));
    }

    private static void assertCompleteSearchResponseShape(SearchResponse response) {
        assertThat(response.query()).isEqualTo(DHAKA_FLOODS_QUERY);
        assertThat(response.mode()).isEqualTo(SearchModes.SEARCH);
        assertThat(response.summary()).isNull();
        assertThat(response.results()).isNotEmpty();

        SearchHit firstHit = response.results().getFirst();
        assertThat(firstHit.id()).isNotBlank();
        assertThat(firstHit.score()).isGreaterThan(0.0);
        assertThat(firstHit.title()).isNotBlank();
        assertThat(firstHit.snippet()).isNotBlank();
        assertThat(firstHit.source()).isNotBlank();
        assertThat(firstHit.language()).isNotBlank();
        assertThat(firstHit.publishedAt()).isNotBlank();

        assertThat(response.latencyMs().embed()).isPositive();
        assertThat(response.latencyMs().search()).isPositive();
        assertThat(response.latencyMs().total()).isPositive();
        assertThat(response.latencyMs().total()).isGreaterThanOrEqualTo(
                response.latencyMs().embed() + response.latencyMs().search());
        assertThat(response.latencyMs().synthesis()).isNull();
    }

    @FunctionalInterface
    private interface EmbedStubConfigurer {
        void configure(WireMockExtension embedSidecar);
    }
}
