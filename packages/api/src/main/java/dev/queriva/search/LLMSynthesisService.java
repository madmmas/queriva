package dev.queriva.search;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * LLM synthesis from retrieved chunks via Ollama (SPEC §8 step 4b, §10, ADR-006).
 */
@Service
public class LLMSynthesisService {

    private static final Logger logger = LoggerFactory.getLogger(LLMSynthesisService.class);

    private final RestClient ollamaRestClient;
    private final String ollamaModel;
    private final RagPromptBuilder ragPromptBuilder;
    private final ObjectMapper objectMapper;

    /**
     * Creates the synthesis service with configured Ollama client and prompt builder.
     */
    public LLMSynthesisService(
            RestClient.Builder restClientBuilder,
            @Value("${ollama.url}") String ollamaUrl,
            @Value("${ollama.model}") String ollamaModel,
            RagPromptBuilder ragPromptBuilder,
            ObjectMapper objectMapper) {
        this.ollamaRestClient = restClientBuilder.baseUrl(ollamaUrl).build();
        this.ollamaModel = ollamaModel;
        this.ragPromptBuilder = ragPromptBuilder;
        this.objectMapper = objectMapper;
    }

    /**
     * Synthesizes an answer from search hits, returning empty when Ollama is unreachable.
     */
    public Optional<String> synthesize(String query, List<SearchHit> hits) {
        if (hits == null || hits.isEmpty()) {
            return Optional.empty();
        }

        String prompt = ragPromptBuilder.buildPrompt(query, hits);
        logger.debug("RAG synthesis prompt length: {} characters", prompt.length());

        try {
            String requestBody = objectMapper.writeValueAsString(
                    new OllamaGenerateRequest(ollamaModel, prompt, RagSynthesisConstants.OLLAMA_STREAM_DISABLED));
            String rawResponse = ollamaRestClient.post()
                    .uri(RagSynthesisConstants.OLLAMA_GENERATE_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            if (rawResponse == null || rawResponse.isBlank()) {
                logger.warn("Ollama returned an empty synthesis response for model '{}'. Degrading gracefully.", ollamaModel);
                return Optional.empty();
            }

            OllamaGenerateResponse response = objectMapper.readValue(rawResponse, OllamaGenerateResponse.class);
            if (response.generatedText() == null || response.generatedText().isBlank()) {
                logger.warn("Ollama returned an empty synthesis response for model '{}'. Degrading gracefully.", ollamaModel);
                return Optional.empty();
            }

            return Optional.of(response.generatedText().trim());
        } catch (RestClientResponseException exception) {
            logger.warn(
                    "Ollama synthesis failed with HTTP {} for model '{}'. Degrading gracefully.",
                    exception.getStatusCode().value(),
                    ollamaModel);
            return Optional.empty();
        } catch (RestClientException exception) {
            logger.warn("Ollama synthesis unreachable for model '{}'. Degrading gracefully.", ollamaModel);
            return Optional.empty();
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            logger.warn("Ollama synthesis response could not be parsed for model '{}'. Degrading gracefully.", ollamaModel);
            return Optional.empty();
        }
    }

    private record OllamaGenerateRequest(String model, String prompt, boolean stream) {
    }

    private record OllamaGenerateResponse(
            @JsonProperty("response") String generatedText,
            @JsonProperty("done") boolean done) {
    }
}
