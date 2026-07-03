package dev.queriva.search;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Optional integration test against a running Ollama instance (issue #17, @Tag slow).
 */
@SpringBootTest
@Tag("slow")
class LLMSynthesisServiceSidecarIT {

    private static final String DEFAULT_OLLAMA_URL = "http://localhost:11434";

    @Autowired
    private LLMSynthesisService llmSynthesisService;

    @Test
    void should_return_non_empty_summary_for_dhaka_floods_query_when_ollama_is_running() {
        Assumptions.assumeTrue(isOllamaHealthy(), "Ollama is not running at " + DEFAULT_OLLAMA_URL);

        Optional<String> summary = llmSynthesisService.synthesize(
                "floods in Dhaka last week",
                List.of(new SearchHit(
                        "cluster-001",
                        0.92,
                        "Buriganga river overflows, floods low-lying Dhaka areas",
                        "Heavy monsoon rains caused the Buriganga river to overflow its banks.",
                        "prothomalo.com",
                        "bn",
                        "2026-06-15T08:30:00Z",
                        "https://example.com/1")));

        assertThat(summary).isPresent();
        assertThat(summary.get()).isNotBlank();
    }

    private static boolean isOllamaHealthy() {
        try {
            RestClient client = RestClient.builder().baseUrl(DEFAULT_OLLAMA_URL).build();
            String body = client.get().uri("/api/tags").retrieve().body(String.class);
            return body != null && body.contains("\"models\"");
        } catch (Exception exception) {
            return false;
        }
    }
}
