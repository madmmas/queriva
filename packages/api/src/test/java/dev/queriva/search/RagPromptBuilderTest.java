package dev.queriva.search;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for RAG prompt construction per SPEC §10.
 */
@Tag("unit")
class RagPromptBuilderTest {

    private static final String QUERY = "floods in Dhaka last week";

    private RagPromptBuilder ragPromptBuilder;

    @BeforeEach
    void setUp() {
        ragPromptBuilder = new RagPromptBuilder();
    }

    @Test
    void should_format_numbered_articles_with_system_instructions_and_user_question() {
        List<SearchHit> hits = List.of(
                new SearchHit(
                        "cluster-001",
                        0.92,
                        "Buriganga river overflows, floods low-lying Dhaka areas",
                        "Heavy monsoon rains caused the Buriganga river to overflow its banks...",
                        "prothomalo.com",
                        "bn",
                        "2026-06-15T08:30:00Z",
                        "https://example.com/1"),
                new SearchHit(
                        "cluster-002",
                        0.88,
                        "200,000 residents displaced by Dhaka flooding, BDRCS responds",
                        "The Bangladesh Red Crescent Society deployed emergency teams...",
                        "thedailystar.net",
                        "en",
                        "2026-06-17T11:15:00Z",
                        "https://example.com/2"));

        String prompt = ragPromptBuilder.buildPrompt(QUERY, hits);

        assertThat(prompt).contains(RagSynthesisConstants.SYSTEM_PROMPT);
        assertThat(prompt).contains(RagSynthesisConstants.ARTICLES_SECTION_HEADER);
        assertThat(prompt).contains("[1] Title: \"Buriganga river overflows, floods low-lying Dhaka areas\"");
        assertThat(prompt).contains("Source: prothomalo.com | Date: 2026-06-15");
        assertThat(prompt).contains("Heavy monsoon rains caused the Buriganga river");
        assertThat(prompt).contains("[2] Title: \"200,000 residents displaced by Dhaka flooding, BDRCS responds\"");
        assertThat(prompt).contains("Source: thedailystar.net | Date: 2026-06-17");
        assertThat(prompt).endsWith(RagSynthesisConstants.USER_QUESTION_PREFIX + QUERY);
    }
}
