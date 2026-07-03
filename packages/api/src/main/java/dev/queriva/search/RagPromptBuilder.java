package dev.queriva.search;

import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Builds the RAG synthesis prompt from retrieved hits per SPEC §10.
 */
@Component
public class RagPromptBuilder {

    /**
     * Formats system instructions, numbered articles, and the user question into an Ollama prompt.
     */
    public String buildPrompt(String query, List<SearchHit> hits) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append(RagSynthesisConstants.SYSTEM_PROMPT)
                .append("\n\n")
                .append(RagSynthesisConstants.ARTICLES_SECTION_HEADER)
                .append('\n');

        for (int index = 0; index < hits.size(); index++) {
            appendArticle(promptBuilder, index + 1, hits.get(index));
        }

        promptBuilder.append(RagSynthesisConstants.USER_QUESTION_PREFIX).append(query.trim());
        return promptBuilder.toString();
    }

    private static void appendArticle(StringBuilder promptBuilder, int articleNumber, SearchHit hit) {
        promptBuilder.append('[')
                .append(articleNumber)
                .append("] Title: \"")
                .append(hit.title())
                .append("\"\n    Source: ")
                .append(hit.source())
                .append(" | Date: ")
                .append(formatPublishedDate(hit.publishedAt()))
                .append("\n    Text: ")
                .append(hit.snippet())
                .append("\n\n");
    }

    private static String formatPublishedDate(String publishedAt) {
        if (publishedAt == null || publishedAt.isBlank()) {
            return "unknown";
        }
        return publishedAt.length() >= 10 ? publishedAt.substring(0, 10) : publishedAt;
    }
}
