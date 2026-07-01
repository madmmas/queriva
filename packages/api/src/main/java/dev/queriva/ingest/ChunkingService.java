package dev.queriva.ingest;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Character-based sliding-window chunker with title prepending (SPEC §7.4, ADR-003, ADR-007).
 */
@Service
public class ChunkingService {

    private static final int MAX_BODY_SNIPPET_CHARS = 500;
    private static final String TITLE_BODY_SEPARATOR = ". ";

    private final int defaultChunkSize;
    private final int defaultOverlap;

    /**
     * Creates the chunker with defaults from SPEC §13 ingest configuration.
     */
    public ChunkingService(
            @Value("${ingest.default-chunk-size}") int defaultChunkSize,
            @Value("${ingest.default-overlap}") int defaultOverlap) {
        this.defaultChunkSize = defaultChunkSize;
        this.defaultOverlap = defaultOverlap;
    }

    /**
     * Splits a document using configured default chunk size and overlap.
     */
    public List<Chunk> chunk(Document document) {
        return chunk(document, defaultChunkSize, defaultOverlap);
    }

    /**
     * Splits a document body into overlapping chunks with title-prefixed embed input.
     */
    public List<Chunk> chunk(Document document, int chunkSize, int overlap) {
        validateChunkParameters(chunkSize, overlap);

        String body = document.body();
        if (body == null || body.isEmpty()) {
            return List.of();
        }

        int stepSize = chunkSize - overlap;
        List<Chunk> chunks = new ArrayList<>();
        int chunkNumber = 1;

        for (int startIndex = 0; startIndex < body.length(); startIndex += stepSize) {
            int endIndex = Math.min(startIndex + chunkSize, body.length());
            String bodySlice = body.substring(startIndex, endIndex);
            String embedInput = document.title() + TITLE_BODY_SEPARATOR + bodySlice;
            String bodySnippet = truncateBodySnippet(bodySlice);
            String pointId = document.id() + "-chunk-" + chunkNumber;

            chunks.add(new Chunk(document.id(), pointId, chunkNumber, embedInput, bodySnippet));

            if (endIndex >= body.length()) {
                break;
            }

            chunkNumber++;
        }

        return chunks;
    }

    /**
     * Produces a single chunk for the full document body when chunking is disabled.
     */
    public List<Chunk> chunkWithoutSplitting(Document document) {
        String body = document.body();
        if (body == null || body.isEmpty()) {
            return List.of();
        }

        String embedInput = document.title() + TITLE_BODY_SEPARATOR + body;
        String bodySnippet = truncateBodySnippet(body);

        return List.of(new Chunk(
                document.id(),
                document.id() + "-chunk-1",
                1,
                embedInput,
                bodySnippet));
    }

    private static String truncateBodySnippet(String bodySlice) {
        if (bodySlice.length() <= MAX_BODY_SNIPPET_CHARS) {
            return bodySlice;
        }
        return bodySlice.substring(0, MAX_BODY_SNIPPET_CHARS);
    }

    private static void validateChunkParameters(int chunkSize, int overlap) {
        if (chunkSize < 1) {
            throw new IllegalArgumentException(
                    "chunk_size must be at least 1. Got " + chunkSize + ".");
        }
        if (overlap < 0) {
            throw new IllegalArgumentException(
                    "overlap must be zero or positive. Got " + overlap + ".");
        }
        if (overlap >= chunkSize) {
            throw new IllegalArgumentException(
                    "overlap must be less than chunk_size so the sliding window advances. "
                            + "Got overlap=" + overlap + ", chunk_size=" + chunkSize + ".");
        }
    }
}
