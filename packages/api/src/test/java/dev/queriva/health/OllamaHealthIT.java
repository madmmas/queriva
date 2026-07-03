package dev.queriva.health;

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
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies GET /api/health Ollama probe against live and unreachable endpoints (issue #16).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Tag("integration")
class OllamaHealthIT {

    private static final String OLLAMA_TAGS_PATH = "/api/tags";
    private static final String EMBED_HEALTH_PATH = "/api/health";

    @RegisterExtension
    static WireMockExtension ollama = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @RegisterExtension
    static WireMockExtension embedSidecar = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Container
    static GenericContainer<?> qdrant = QdrantTestcontainersSupport.newQdrantContainer();

    @Autowired
    private DependencyHealthService dependencyHealthService;

    @DynamicPropertySource
    static void configureDependencies(DynamicPropertyRegistry registry) {
        String host = qdrant.getHost();
        int restPort = qdrant.getMappedPort(QdrantTestcontainersSupport.REST_PORT);
        int grpcPort = qdrant.getMappedPort(QdrantTestcontainersSupport.GRPC_PORT);

        registry.add("qdrant.url", () -> "http://" + host + ":" + restPort);
        registry.add("qdrant.grpc-port", () -> String.valueOf(grpcPort));
        registry.add("ollama.url", ollama::baseUrl);
        registry.add("embed-sidecar.url", embedSidecar::baseUrl);
    }

    @BeforeEach
    void setUp() {
        ollama.resetAll();
        embedSidecar.resetAll();
        stubEmbedSidecarHealthy();
    }

    @Test
    void should_report_ollama_connected_when_tags_endpoint_responds() {
        ollama.stubFor(get(urlEqualTo(OLLAMA_TAGS_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"models\":[{\"name\":\"mistral\"}]}")));

        HealthResponse health = dependencyHealthService.checkHealth();

        assertThat(health.ollama()).isEqualTo("connected");
        assertThat(health.qdrant()).isEqualTo("connected");
    }

    @Test
    void should_report_ollama_disconnected_when_tags_endpoint_is_unreachable() {
        ollama.stubFor(get(urlEqualTo(OLLAMA_TAGS_PATH))
                .willReturn(aResponse().withStatus(503)));

        HealthResponse health = dependencyHealthService.checkHealth();

        assertThat(health.ollama()).isEqualTo("disconnected");
    }

    private static void stubEmbedSidecarHealthy() {
        embedSidecar.stubFor(get(urlEqualTo(EMBED_HEALTH_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"ok\",\"models_loaded\":[]}")));
    }
}
