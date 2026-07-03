package dev.queriva.support;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import dev.queriva.search.RagSynthesisConstants;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

/**
 * WireMock embed-sidecar stubs for search integration tests (issue #15).
 */
public final class SearchIntegrationTestSupport {

    /** Embed sidecar POST path (SPEC §6). */
    public static final String EMBED_API_PATH = "/api/embed";

    /** Integration vector size used with Testcontainers Qdrant collections. */
    public static final int INTEGRATION_VECTOR_SIZE = 2;

    /** Performance baseline for search mode (SPEC §15, test-quality.mdc E5). */
    public static final long SEARCH_MODE_BASELINE_MILLIS = 500L;

    /** Performance ceiling for RAG mode on CPU (test-quality.mdc E5; SPEC §15 ~2–5s typical). */
    public static final long RAG_MODE_BASELINE_MILLIS = 10_000L;

    /** Ollama generate path used by RAG synthesis (SPEC §8 step 4b). */
    public static final String OLLAMA_GENERATE_PATH = RagSynthesisConstants.OLLAMA_GENERATE_PATH;

    /** Stub summary citing the primary fixture article for WireMock Ollama tests. */
    public static final String STUB_RAG_SUMMARY =
            "Buriganga river overflows caused flooding in low-lying Dhaka areas last week.";

    /** Primary article id in {@code fixtures/news_radar_dhaka_floods.json}. */
    public static final String FIRST_ARTICLE_ID = "cluster-001";

    /** Primary article title in {@code fixtures/news_radar_dhaka_floods.json}. */
    public static final String FIRST_ARTICLE_TITLE =
            "Buriganga river overflows, floods low-lying Dhaka areas";

    /** Canonical Dhaka floods query used across search integration tests. */
    public static final String DHAKA_FLOODS_QUERY = "floods in Dhaka last week";

    private static final String JSON_CONTENT_TYPE = "application/json";

    private SearchIntegrationTestSupport() {
    }

    /**
     * Stubs embed responses so article #1 ranks highest for the Dhaka floods query.
     */
    public static void stubRankedEmbedResponses(WireMockExtension embedSidecar) {
        embedSidecar.stubFor(post(urlEqualTo(EMBED_API_PATH))
                .withRequestBody(containing(DHAKA_FLOODS_QUERY))
                .willReturn(jsonVectorResponse("[1.0,0.0]")));

        embedSidecar.stubFor(post(urlEqualTo(EMBED_API_PATH))
                .withRequestBody(containing("Buriganga"))
                .willReturn(jsonVectorResponse("[1.0,0.0]")));

        embedSidecar.stubFor(post(urlEqualTo(EMBED_API_PATH))
                .atPriority(10)
                .willReturn(jsonVectorResponse("[0.2,0.8]")));
    }

    /**
     * Stubs embed responses with low cosine similarity between query and document vectors.
     */
    public static void stubLowSimilarityEmbedResponses(WireMockExtension embedSidecar) {
        embedSidecar.stubFor(post(urlEqualTo(EMBED_API_PATH))
                .withRequestBody(containing(DHAKA_FLOODS_QUERY))
                .willReturn(jsonVectorResponse("[1.0,0.0]")));

        embedSidecar.stubFor(post(urlEqualTo(EMBED_API_PATH))
                .atPriority(10)
                .willReturn(jsonVectorResponse("[0.0,1.0]")));
    }

    /**
     * Stubs a uniform embed vector for all requests (filter and top-k tests).
     */
    public static void stubUniformEmbedResponses(WireMockExtension embedSidecar) {
        embedSidecar.stubFor(post(urlEqualTo(EMBED_API_PATH))
                .willReturn(jsonVectorResponse("[0.1,0.2]")));
    }

    /**
     * Stubs a successful Ollama generate response for RAG integration tests (issue #18).
     */
    public static void stubOllamaGenerateResponse(WireMockExtension ollama, String summaryText) {
        String escapedSummary = summaryText.replace("\\", "\\\\").replace("\"", "\\\"");
        ollama.stubFor(post(urlEqualTo(OLLAMA_GENERATE_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", JSON_CONTENT_TYPE)
                        .withBody("{\"response\":\"" + escapedSummary + "\",\"done\":true}")));
    }

    private static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder jsonVectorResponse(
            String vectorJson) {
        return aResponse()
                .withStatus(200)
                .withHeader("Content-Type", JSON_CONTENT_TYPE)
                .withBody("{\"vector\":" + vectorJson + ",\"dimensions\":" + INTEGRATION_VECTOR_SIZE + "}");
    }
}
