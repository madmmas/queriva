package dev.queriva.search;

import java.util.List;

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
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Assumptions;
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
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static dev.queriva.support.SearchIntegrationTestSupport.DHAKA_FLOODS_QUERY;
import static dev.queriva.support.SearchIntegrationTestSupport.RAG_MODE_BASELINE_MILLIS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end RAG validation against a live Ollama instance (issue #18, {@code @Tag("slow")}).
 *
 * <p>Requires Ollama with the configured model (default {@code mistral}) at {@code OLLAMA_URL}
 * or {@code http://localhost:11434}. Run after {@code docker compose up} and {@code make ollama-pull}.
 *
 * <p>Baseline latency: SPEC §15 documents ~2–5s for RAG synthesis on CPU; test-quality.mdc E5
 * allows up to {@value SearchIntegrationTestSupport#RAG_MODE_BASELINE_MILLIS}ms for the full request.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("slow")
class RagModeSlowIT {

    private static final String COLLECTION = "news_radar_rag_mode_slow_it";
    private static final String MODEL = "LaBSE";
    private static final String DISTANCE = "Cosine";
    private static final double AUTO_ACCEPT_DISABLED = 0.99;
    private static final String OLLAMA_TAGS_PATH = "/api/tags";
    private static final String DEFAULT_OLLAMA_URL = "http://localhost:11434";

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
        String ollamaUrl = System.getenv().getOrDefault("OLLAMA_URL", DEFAULT_OLLAMA_URL);

        registry.add("qdrant.url", () -> "http://" + host + ":" + restPort);
        registry.add("qdrant.grpc-port", () -> String.valueOf(grpcPort));
        registry.add("embed-sidecar.url", embedSidecar::baseUrl);
        registry.add("ollama.url", () -> ollamaUrl);
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
        Assumptions.assumeTrue(
                isOllamaHealthy(),
                "Ollama is not running — start docker compose and run make ollama-pull");

        embedSidecar.resetAll();
        SearchIntegrationTestSupport.stubRankedEmbedResponses(embedSidecar);
        recreateCollection();
        ingestFixture();
    }

    @Test
    void should_return_non_empty_summary_for_dhaka_floods_query_against_live_ollama() {
        SearchResponse response = search(ragRequest());

        assertThat(response.summary()).isNotBlank();
        assertThat(response.results()).isNotEmpty();
    }

    @Test
    void should_populate_synthesis_latency_against_live_ollama() {
        SearchResponse response = search(ragRequest());

        assertThat(response.latencyMs().synthesis()).isNotNull();
        assertThat(response.latencyMs().synthesis()).isPositive();
        assertThat(response.latencyMs().total()).isGreaterThan(response.latencyMs().synthesis());
    }

    @Test
    void should_complete_rag_mode_within_performance_baseline_against_live_ollama() {
        SearchResponse response = search(ragRequest());

        assertThat(response.latencyMs().total()).isLessThan(RAG_MODE_BASELINE_MILLIS);
        assertThat(response.latencyMs().synthesis()).isPositive();
    }

    @Test
    void should_differ_in_summary_presence_between_search_and_rag_modes_against_live_ollama() {
        SearchResponse searchModeResponse = search(searchRequest());
        SearchResponse ragModeResponse = search(ragRequest());

        assertThat(searchModeResponse.summary()).isNull();
        assertThat(searchModeResponse.latencyMs().synthesis()).isNull();
        assertThat(ragModeResponse.summary()).isNotBlank();
        assertThat(ragModeResponse.latencyMs().synthesis()).isPositive();
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

    private static SearchRequest searchRequest() {
        return new SearchRequest(
                DHAKA_FLOODS_QUERY,
                COLLECTION,
                10,
                0.60,
                SearchModes.SEARCH,
                null);
    }

    private static SearchRequest ragRequest() {
        return new SearchRequest(
                DHAKA_FLOODS_QUERY,
                COLLECTION,
                10,
                0.60,
                SearchModes.RAG,
                null);
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

    private static boolean isOllamaHealthy() {
        String ollamaUrl = System.getenv().getOrDefault("OLLAMA_URL", DEFAULT_OLLAMA_URL);
        try {
            RestClient client = RestClient.builder().baseUrl(ollamaUrl).build();
            String body = client.get().uri(OLLAMA_TAGS_PATH).retrieve().body(String.class);
            return body != null && body.contains("\"models\"");
        } catch (Exception exception) {
            return false;
        }
    }
}
