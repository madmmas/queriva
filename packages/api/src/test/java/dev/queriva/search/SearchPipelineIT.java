package dev.queriva.search;

import java.util.List;
import java.util.stream.Stream;

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
 * Full search-mode integration tests using the News Radar fixture corpus (issue #14).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("integration")
class SearchPipelineIT {

    private static final String COLLECTION = "news_radar_search_pipeline_it";
    private static final String MODEL = "LaBSE";
    private static final int VECTOR_SIZE = 2;
    private static final String DISTANCE = "Cosine";
    private static final String QUERY = "floods in Dhaka last week";
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
    }

    @BeforeEach
    void setUp() {
        embedSidecar.resetAll();
        stubEmbedResponse();
        recreateCollection();
        ingestFixture();
    }

    @Test
    void should_return_at_least_four_results_for_dhaka_floods_query() {
        ResponseEntity<SearchResponse> response = search(new SearchRequest(
                QUERY, COLLECTION, 10, 0.60, SearchModes.SEARCH, null));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().results()).hasSizeGreaterThanOrEqualTo(4);
        assertThat(response.getBody().results().getFirst().score()).isGreaterThan(0.60);
    }

    @Test
    void should_return_404_when_collection_does_not_exist() {
        ResponseEntity<String> response = testRestTemplate.postForEntity(
                "/api/search",
                new SearchRequest(QUERY, "missing_collection", 10, 0.60, SearchModes.SEARCH, null),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("error");
    }

    @Test
    void should_match_spec_section_six_response_contract_in_search_mode() throws Exception {
        ResponseEntity<String> response = testRestTemplate.postForEntity(
                "/api/search",
                new SearchRequest(QUERY, COLLECTION, 10, 0.60, SearchModes.SEARCH, null),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode body = OBJECT_MAPPER.readTree(response.getBody());
        assertThat(body.get("query").asText()).isEqualTo(QUERY);
        assertThat(body.get("mode").asText()).isEqualTo(SearchModes.SEARCH);
        assertThat(body.get("summary").isNull()).isTrue();
        assertThat(body.get("results").isArray()).isTrue();
        assertThat(body.get("results")).isNotEmpty();

        JsonNode firstResult = body.get("results").get(0);
        assertThat(firstResult.get("id").asText()).isNotBlank();
        assertThat(firstResult.get("score").asDouble()).isGreaterThan(0.0);
        assertThat(firstResult.get("title").asText()).isNotBlank();
        assertThat(firstResult.get("snippet").asText()).isNotBlank();
        assertThat(firstResult.get("source").asText()).isNotBlank();
        assertThat(firstResult.get("language").asText()).isNotBlank();
        assertThat(firstResult.get("published_at").asText()).isNotBlank();
        assertThat(firstResult.has("url")).isTrue();

        JsonNode latency = body.get("latency_ms");
        assertThat(latency.get("embed").asLong()).isPositive();
        assertThat(latency.get("search").asLong()).isPositive();
        assertThat(latency.get("total").asLong()).isPositive();
        assertThat(latency.get("total").asLong()).isGreaterThanOrEqualTo(
                latency.get("embed").asLong() + latency.get("search").asLong());
        assertThat(latency.has("synthesis")).isFalse();
    }

    @Test
    void should_populate_positive_latency_breakdown_fields() {
        SearchResponse body = search(new SearchRequest(QUERY, COLLECTION, 10, 0.60, SearchModes.SEARCH, null))
                .getBody();

        assertThat(body).isNotNull();
        assertThat(body.latencyMs().embed()).isPositive();
        assertThat(body.latencyMs().search()).isPositive();
        assertThat(body.latencyMs().total()).isPositive();
        assertThat(body.latencyMs().synthesis()).isNull();
    }

    @ParameterizedTest
    @MethodSource("filterCombinations")
    void should_filter_results_correctly(SearchFilters filters, int minimumExpectedCount) {
        SearchResponse body = search(new SearchRequest(QUERY, COLLECTION, 10, 0.60, SearchModes.SEARCH, filters))
                .getBody();

        assertThat(body).isNotNull();
        assertThat(body.results()).hasSizeGreaterThanOrEqualTo(minimumExpectedCount);

        if (filters.language() != null) {
            assertThat(body.results()).extracting(SearchHit::language).containsOnly(filters.language());
        }
    }

    static Stream<Arguments> filterCombinations() {
        return Stream.of(
                Arguments.of(new SearchFilters("bn", null, null, null), 1),
                Arguments.of(new SearchFilters("en", null, null, null), 1),
                Arguments.of(new SearchFilters(null, "2026-06-15", "2026-06-17", null), 1),
                Arguments.of(new SearchFilters(null, null, null, "national"), 1));
    }

    private ResponseEntity<SearchResponse> search(SearchRequest request) {
        return testRestTemplate.postForEntity("/api/search", request, SearchResponse.class);
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
