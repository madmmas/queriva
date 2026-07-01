package dev.queriva.ingest;

import java.nio.file.Files;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.queriva.support.FixturePaths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for character-based sliding-window chunking (ADR-003, ADR-007).
 */
@Tag("unit")
class ChunkingServiceTest {

    private static final int CHUNK_SIZE = 512;
    private static final int OVERLAP = 64;
    private static final int STEP_SIZE = CHUNK_SIZE - OVERLAP;
    private static final String DOCUMENT_ID = "doc-flood-001";
    private static final String DOCUMENT_TITLE = "Floods in Dhaka";

    private ChunkingService chunkingService;

    @BeforeEach
    void setUp() {
        chunkingService = new ChunkingService(CHUNK_SIZE, OVERLAP);
    }

    @Test
    void should_produce_five_chunks_when_body_is_2000_chars_with_512_size_and_64_overlap() {
        String body = "x".repeat(2000);
        Document document = new Document(DOCUMENT_ID, DOCUMENT_TITLE, body);

        List<Chunk> chunks = chunkingService.chunk(document, CHUNK_SIZE, OVERLAP);

        assertThat(chunks).hasSize(5);
    }

    @Test
    void should_advance_start_index_by_448_chars_between_consecutive_chunks() {
        String body = "x".repeat(2000);
        Document document = new Document(DOCUMENT_ID, DOCUMENT_TITLE, body);

        List<Chunk> chunks = chunkingService.chunk(document, CHUNK_SIZE, OVERLAP);

        String titlePrefix = DOCUMENT_TITLE + ". ";
        assertThat(chunks.get(0).embedInput()).isEqualTo(titlePrefix + body.substring(0, CHUNK_SIZE));
        assertThat(chunks.get(1).embedInput()).isEqualTo(titlePrefix + body.substring(STEP_SIZE, STEP_SIZE + CHUNK_SIZE));
    }

    @Test
    void should_prepend_title_to_embed_input_for_every_chunk() {
        String body = "x".repeat(2000);
        Document document = new Document(DOCUMENT_ID, DOCUMENT_TITLE, body);

        List<Chunk> chunks = chunkingService.chunk(document, CHUNK_SIZE, OVERLAP);

        assertThat(chunks)
                .extracting(Chunk::embedInput)
                .allMatch(embedInput -> embedInput.startsWith(DOCUMENT_TITLE + ". "));
    }

    @Test
    void should_exclude_title_from_body_snippet_and_cap_at_500_chars() {
        String body = "y".repeat(2000);
        Document document = new Document(DOCUMENT_ID, DOCUMENT_TITLE, body);

        List<Chunk> chunks = chunkingService.chunk(document, CHUNK_SIZE, OVERLAP);

        assertThat(chunks)
                .extracting(Chunk::bodySnippet)
                .allMatch(snippet -> !snippet.startsWith(DOCUMENT_TITLE))
                .allMatch(snippet -> snippet.length() <= 500);
    }

    @Test
    void should_return_no_chunks_when_body_is_empty() {
        Document emptyBodyDocument = new Document(DOCUMENT_ID, DOCUMENT_TITLE, "");

        List<Chunk> chunks = chunkingService.chunk(emptyBodyDocument, CHUNK_SIZE, OVERLAP);

        assertThat(chunks).isEmpty();
    }

    @Test
    void should_return_no_chunks_when_body_is_null() {
        Document nullBodyDocument = new Document(DOCUMENT_ID, DOCUMENT_TITLE, null);

        List<Chunk> chunks = chunkingService.chunk(nullBodyDocument, CHUNK_SIZE, OVERLAP);

        assertThat(chunks).isEmpty();
    }

    @Test
    void should_produce_one_chunk_when_body_is_shorter_than_chunk_size() {
        String shortBody = "Short flood update from Dhaka.";
        Document document = new Document(DOCUMENT_ID, DOCUMENT_TITLE, shortBody);

        List<Chunk> chunks = chunkingService.chunk(document, CHUNK_SIZE, OVERLAP);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).embedInput()).isEqualTo(DOCUMENT_TITLE + ". " + shortBody);
        assertThat(chunks.get(0).bodySnippet()).isEqualTo(shortBody);
        assertThat(chunks.get(0).pointId()).isEqualTo(DOCUMENT_ID + "-chunk-1");
    }

    @Test
    void should_share_document_id_across_all_chunks_from_one_document() {
        String body = "z".repeat(2000);
        Document document = new Document(DOCUMENT_ID, DOCUMENT_TITLE, body);

        List<Chunk> chunks = chunkingService.chunk(document, CHUNK_SIZE, OVERLAP);

        assertThat(chunks)
                .extracting(Chunk::documentId)
                .containsOnly(DOCUMENT_ID);
    }

    @Test
    void should_chunk_first_article_from_news_radar_fixture_with_expected_point_ids() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode articles = objectMapper.readTree(Files.readString(
                FixturePaths.repoFixture("news_radar_dhaka_floods.json")));
        JsonNode firstArticle = articles.get(0);

        Document document = new Document(
                firstArticle.get("id").asText(),
                firstArticle.get("title").asText(),
                firstArticle.get("body").asText());

        List<Chunk> chunks = chunkingService.chunk(document);

        assertThat(chunks).isNotEmpty();
        assertThat(chunks)
                .extracting(Chunk::documentId)
                .containsOnly("cluster-001");
        assertThat(chunks.get(0).pointId()).isEqualTo("cluster-001-chunk-1");
        assertThat(chunks.get(0).embedInput()).startsWith(firstArticle.get("title").asText() + ". ");
        String body = firstArticle.get("body").asText();
        int firstSliceLength = Math.min(CHUNK_SIZE, body.length());
        int expectedSnippetLength = Math.min(500, firstSliceLength);
        assertThat(chunks.get(0).bodySnippet()).isEqualTo(body.substring(0, expectedSnippetLength));
    }

    @Test
    void should_reject_overlap_greater_than_or_equal_to_chunk_size() {
        Document document = new Document(DOCUMENT_ID, DOCUMENT_TITLE, "body");

        assertThatThrownBy(() -> chunkingService.chunk(document, CHUNK_SIZE, CHUNK_SIZE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("overlap must be less than chunk_size");
    }
}
