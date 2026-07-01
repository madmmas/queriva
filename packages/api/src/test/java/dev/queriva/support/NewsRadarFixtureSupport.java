package dev.queriva.support;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.queriva.ingest.IngestDocumentDto;

/**
 * Loads the canonical News Radar integration test corpus from the monorepo fixture.
 */
public final class NewsRadarFixtureSupport {

    /** Fixture file name at repository root (test-quality.mdc E1). */
    public static final String FIXTURE_FILE_NAME = "news_radar_dhaka_floods.json";

    /** Article count in the Dhaka floods demo corpus. */
    public static final int DOCUMENT_COUNT = 8;

    /** Chunk points produced with chunk_size=512 and overlap=64. */
    public static final int CHUNKED_POINT_COUNT = 13;

    /** Chunk points when chunking is disabled (one chunk per document). */
    public static final int UNCHUNKED_POINT_COUNT = 8;

    /** Internal metadata point written on first ingest per collection. */
    public static final int METADATA_POINT_COUNT = 1;

    /** Bangla articles in the fixture. */
    public static final int BANGLA_DOCUMENT_COUNT = 4;

    /** English articles in the fixture. */
    public static final int ENGLISH_DOCUMENT_COUNT = 4;

    /** Performance baseline for ingesting the full fixture (SPEC §15, issue #8). */
    public static final long INGEST_BASELINE_MILLIS = 30_000L;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private NewsRadarFixtureSupport() {
    }

    /**
     * Loads all documents from {@code fixtures/news_radar_dhaka_floods.json}.
     */
    public static List<IngestDocumentDto> loadDocuments() {
        try {
            String fixtureJson = Files.readString(FixturePaths.repoFixture(FIXTURE_FILE_NAME));
            List<IngestDocumentDto> documents = OBJECT_MAPPER.readValue(
                    fixtureJson, new TypeReference<List<IngestDocumentDto>>() {
                    });

            if (documents.size() != DOCUMENT_COUNT) {
                throw new IllegalStateException(
                        "Expected " + DOCUMENT_COUNT + " fixture documents but found " + documents.size());
            }

            return documents;
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to load fixture '" + FIXTURE_FILE_NAME + "'. Run tests from packages/api.",
                    exception);
        }
    }
}
