package dev.queriva.search;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.web.client.RestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for LLMSynthesisService with WireMock Ollama stubs (issue #17).
 */
@Tag("unit")
@Execution(ExecutionMode.SAME_THREAD)
class LLMSynthesisServiceTest {

    private static final String MODEL = "mistral";
    private static final String QUERY = "floods in Dhaka last week";

    @RegisterExtension
    static WireMockExtension ollama = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private LLMSynthesisService llmSynthesisService;

    @BeforeEach
    void setUp() {
        ollama.resetAll();
        llmSynthesisService = new LLMSynthesisService(
                RestClient.builder(),
                ollama.baseUrl(),
                MODEL,
                new RagPromptBuilder(),
                new ObjectMapper());
    }

    @Test
    void should_return_summary_when_ollama_generate_returns_200() {
        ollama.stubFor(post(urlEqualTo(RagSynthesisConstants.OLLAMA_GENERATE_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                                "{\"response\":\"Buriganga river floods Dhaka low-lying areas.\",\"done\":true}")));

        Optional<String> summary = llmSynthesisService.synthesize(QUERY, sampleHits());

        assertThat(summary).isPresent();
        assertThat(summary.get()).contains("Buriganga");
        ollama.verify(1, postRequestedFor(urlEqualTo(RagSynthesisConstants.OLLAMA_GENERATE_PATH)));
    }

    @Test
    void should_return_empty_summary_when_ollama_returns_503() {
        ollama.stubFor(post(urlEqualTo(RagSynthesisConstants.OLLAMA_GENERATE_PATH))
                .willReturn(aResponse().withStatus(503)));

        Optional<String> summary = llmSynthesisService.synthesize(QUERY, sampleHits());

        assertThat(summary).isEmpty();
        ollama.verify(1, postRequestedFor(urlEqualTo(RagSynthesisConstants.OLLAMA_GENERATE_PATH)));
    }

    @Test
    void should_return_empty_summary_when_hits_list_is_empty() {
        Optional<String> summary = llmSynthesisService.synthesize(QUERY, List.of());

        assertThat(summary).isEmpty();
        ollama.verify(0, postRequestedFor(urlEqualTo(RagSynthesisConstants.OLLAMA_GENERATE_PATH)));
    }

    private static List<SearchHit> sampleHits() {
        return List.of(new SearchHit(
                "cluster-001",
                0.92,
                "Buriganga river overflows, floods low-lying Dhaka areas",
                "Heavy monsoon rains caused flooding across Dhaka.",
                "prothomalo.com",
                "bn",
                "2026-06-15T08:30:00Z",
                "https://example.com/1"));
    }
}
