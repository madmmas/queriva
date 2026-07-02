package dev.queriva.search;

import java.time.Duration;
import java.util.stream.IntStream;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import dev.queriva.ingest.CollectionEmbeddingModelService;
import dev.queriva.ingest.EmbedSidecarException;
import dev.queriva.ingest.EmbeddingModelMismatchException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for QueryEmbeddingService with WireMock (issue #13, test-quality.mdc B6).
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
class QueryEmbeddingServiceTest {

    private static final String MODEL = "LaBSE";
    private static final String COLLECTION = "news_radar";
    private static final String EMBED_PATH = QueryEmbeddingConstants.EMBED_API_PATH;

    @RegisterExtension
    static WireMockExtension embedSidecar = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Mock
    private CollectionEmbeddingModelService collectionEmbeddingModelService;

    private QueryEmbeddingService queryEmbeddingService;

    @BeforeEach
    void setUp() {
        embedSidecar.resetAll();
        queryEmbeddingService = new QueryEmbeddingService(
                restClientBuilderWithTimeouts(Duration.ofSeconds(5), Duration.ofSeconds(30)),
                embedSidecar.baseUrl(),
                MODEL,
                collectionEmbeddingModelService);
    }

    @Test
    void should_return_vector_when_embed_sidecar_returns_200() {
        int dimensions = QueryEmbeddingConstants.LABSE_VECTOR_DIMENSIONS;
        embedSidecar.stubFor(post(urlEqualTo(EMBED_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(embedResponseBody(dimensions))));

        float[] vector = queryEmbeddingService.embed("floods in Dhaka", MODEL);

        assertThat(vector).hasSize(dimensions);
        assertThat(vector[0]).isEqualTo(0.0f);
        assertThat(vector[1]).isEqualTo(0.1f);
        embedSidecar.verify(1, postRequestedFor(urlEqualTo(EMBED_PATH)));
    }

    @Test
    void should_retry_once_when_embed_sidecar_returns_503_then_succeeds() {
        embedSidecar.stubFor(post(urlEqualTo(EMBED_PATH))
                .inScenario("retry-once")
                .whenScenarioStateIs(Scenario.STARTED)
                .willSetStateTo("second-attempt")
                .willReturn(aResponse().withStatus(503)));

        embedSidecar.stubFor(post(urlEqualTo(EMBED_PATH))
                .inScenario("retry-once")
                .whenScenarioStateIs("second-attempt")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(embedResponseBody(3))));

        float[] vector = queryEmbeddingService.embed("floods in Dhaka", MODEL);

        assertThat(vector).hasSize(3);
        embedSidecar.verify(2, postRequestedFor(urlEqualTo(EMBED_PATH)));
    }

    @Test
    void should_throw_embed_sidecar_exception_when_503_persists_after_retry() {
        embedSidecar.stubFor(post(urlEqualTo(EMBED_PATH))
                .willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(() -> queryEmbeddingService.embed("floods in Dhaka", MODEL))
                .isInstanceOf(EmbedSidecarException.class)
                .hasMessageContaining("HTTP 503")
                .hasMessageContaining(MODEL);

        embedSidecar.verify(2, postRequestedFor(urlEqualTo(EMBED_PATH)));
    }

    @Test
    void should_not_retry_when_embed_sidecar_returns_400() {
        embedSidecar.stubFor(post(urlEqualTo(EMBED_PATH))
                .willReturn(aResponse().withStatus(400)));

        assertThatThrownBy(() -> queryEmbeddingService.embed("floods in Dhaka", MODEL))
                .isInstanceOf(EmbedSidecarException.class)
                .hasMessageContaining("HTTP 400");

        embedSidecar.verify(1, postRequestedFor(urlEqualTo(EMBED_PATH)));
    }

    @Test
    void should_throw_embed_sidecar_exception_when_read_timeout_exceeds_configured_limit() {
        QueryEmbeddingService timeoutService = new QueryEmbeddingService(
                restClientBuilderWithTimeouts(Duration.ofMillis(200), Duration.ofMillis(500)),
                embedSidecar.baseUrl(),
                MODEL,
                collectionEmbeddingModelService);

        embedSidecar.stubFor(post(urlEqualTo(EMBED_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(700)
                        .withHeader("Content-Type", "application/json")
                        .withBody(embedResponseBody(2))));

        assertThatThrownBy(() -> timeoutService.embed("floods in Dhaka", MODEL))
                .isInstanceOf(EmbedSidecarException.class)
                .hasMessageContaining("unreachable");

        embedSidecar.verify(2, postRequestedFor(urlEqualTo(EMBED_PATH)));
    }

    @Test
    void should_use_default_model_when_model_argument_is_blank() {
        embedSidecar.stubFor(post(urlEqualTo(EMBED_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(embedResponseBody(2))));

        float[] vector = queryEmbeddingService.embed("floods in Dhaka", "  ");

        assertThat(vector).hasSize(2);
        embedSidecar.verify(postRequestedFor(urlEqualTo(EMBED_PATH))
                .withRequestBody(containing("\"model\":\"LaBSE\"")));
    }

    @Test
    void should_delegate_model_validation_to_collection_embedding_model_service() {
        queryEmbeddingService.validateEmbeddingModel(COLLECTION, MODEL);

        verify(collectionEmbeddingModelService).validateModelForSearch(COLLECTION, MODEL);
    }

    @Test
    void should_propagate_embedding_model_mismatch_from_collection_validation() {
        doThrow(new EmbeddingModelMismatchException(COLLECTION, "all-MiniLM-L6-v2", MODEL))
                .when(collectionEmbeddingModelService)
                .validateModelForSearch(COLLECTION, "all-MiniLM-L6-v2");

        assertThatThrownBy(() -> queryEmbeddingService.validateEmbeddingModel(COLLECTION, "all-MiniLM-L6-v2"))
                .isInstanceOf(EmbeddingModelMismatchException.class)
                .hasMessageContaining("Embedding model mismatch");
    }

    private static RestClient.Builder restClientBuilderWithTimeouts(
            Duration connectTimeout,
            Duration readTimeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        return RestClient.builder().requestFactory(requestFactory);
    }

    private static String embedResponseBody(int dimensions) {
        String vectorValues = IntStream.range(0, dimensions)
                .mapToObj(index -> String.valueOf((index % 10) / 10.0f))
                .reduce((left, right) -> left + "," + right)
                .orElse("");
        return "{\"vector\":[" + vectorValues + "],\"dimensions\":" + dimensions + "}";
    }
}
