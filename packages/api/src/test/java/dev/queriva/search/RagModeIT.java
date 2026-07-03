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

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static dev.queriva.support.SearchIntegrationTestSupport.DHAKA_FLOODS_QUERY;
import static dev.queriva.support.SearchIntegrationTestSupport.FIRST_ARTICLE_TITLE;
import static dev.queriva.support.SearchIntegrationTestSupport.STUB_RAG_SUMMARY;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full RAG pipeline integration via POST /api/search with WireMock Ollama (issue #18).
 *
 * <p>Baseline latency for real Ollama synthesis is ~2–5s per SPEC §15; WireMock stubs complete
 * in milliseconds. See {@link RagModeSlowIT} for live Ollama validation ({@code @Tag("slow")}).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("integration")
class RagModeIT {

    private static final String COLLECTION = "news_radar_rag_mode_it";
    private static final String MODEL = "LaBSE";
    private static final String DISTANCE = "Cosine";
    private static final double AUTO_ACCEPT_DISABLED = 0.99;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @RegisterExtension
    static WireMockExtension embedSidecar = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @RegisterExtension
    static WireMockExtension ollama = WireMockExtension.newInstance()
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
        registry.add("ollama.url", ollama::baseUrl);
        registry.add("search.max-score-auto-accept", () -> String.valueOf(AUTO_ACCEPT_DISABLED));
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
        ollama.resetAll();
        SearchIntegrationTestSupport.stubRankedEmbedResponses(embedSidecar);
        SearchIntegrationTestSupport.stubOllamaGenerateResponse(ollama, STUB_RAG_SUMMARY);
        recreateCollection();
        ingestFixture();
    }

    @Test
    void should_return_non_empty_summary_for_dhaka_floods_query_in_rag_mode() {
        SearchResponse response = search(ragRequest(10, 0.60, null));

        assertThat(response.summary()).isNotBlank();
        assertThat(response.summary()).contains("Buriganga");
    }

    @Test
    void should_populate_positive_synthesis_latency_in_rag_mode() {
        SearchResponse response = search(ragRequest(10, 0.60, null));

        assertThat(response.latencyMs().synthesis()).isNotNull();
        assertThat(response.latencyMs().synthesis()).isPositive();
    }

    @Test
    void should_have_total_latency_greater_than_synthesis_latency_in_rag_mode() {
        SearchResponse response = search(ragRequest(10, 0.60, null));

        assertThat(response.latencyMs().total()).isGreaterThan(response.latencyMs().synthesis());
        assertThat(response.latencyMs().embed()).isPositive();
        assertThat(response.latencyMs().search()).isPositive();
    }

    @Test
    void should_return_null_summary_in_search_mode_and_non_empty_summary_in_rag_mode_for_same_query()
            throws Exception {
        SearchResponse searchModeResponse = search(searchRequest(10, 0.60, null));
        SearchResponse ragModeResponse = search(ragRequest(10, 0.60, null));

        assertThat(searchModeResponse.mode()).isEqualTo(SearchModes.SEARCH);
        assertThat(searchModeResponse.summary()).isNull();
        assertThat(searchModeResponse.latencyMs().synthesis()).isNull();

        assertThat(ragModeResponse.mode()).isEqualTo(SearchModes.RAG);
        assertThat(ragModeResponse.summary()).isNotBlank();
        assertThat(ragModeResponse.latencyMs().synthesis()).isPositive();

        ResponseEntity<String> rawSearch = testRestTemplate.postForEntity(
                "/api/search",
                searchRequest(10, 0.60, null),
                String.class);
        JsonNode searchBody = OBJECT_MAPPER.readTree(rawSearch.getBody());
        assertThat(searchBody.get("summary").isNull()).isTrue();
        assertThat(searchBody.get("latency_ms").has("synthesis")).isFalse();

        ResponseEntity<String> rawRag = testRestTemplate.postForEntity(
                "/api/search",
                ragRequest(10, 0.60, null),
                String.class);
        JsonNode ragBody = OBJECT_MAPPER.readTree(rawRag.getBody());
        assertThat(ragBody.get("summary").asText()).contains("Buriganga");
        assertThat(ragBody.get("latency_ms").get("synthesis").asLong()).isPositive();
    }

    @Test
    void should_cite_fixture_article_title_in_rag_summary_from_stubbed_ollama() {
        SearchResponse response = search(ragRequest(10, 0.60, null));

        assertThat(response.results()).isNotEmpty();
        assertThat(response.results().getFirst().title()).isEqualTo(FIRST_ARTICLE_TITLE);
        assertThat(response.summary()).contains("Buriganga");
    }

    private static SearchRequest searchRequest(int topK, double minScore, SearchFilters filters) {
        return new SearchRequest(
                DHAKA_FLOODS_QUERY,
                COLLECTION,
                topK,
                minScore,
                SearchModes.SEARCH,
                filters);
    }

    private static SearchRequest ragRequest(int topK, double minScore, SearchFilters filters) {
        return new SearchRequest(
                DHAKA_FLOODS_QUERY,
                COLLECTION,
                topK,
                minScore,
                SearchModes.RAG,
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
}
