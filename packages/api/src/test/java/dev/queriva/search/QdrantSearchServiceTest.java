package dev.queriva.search;

import java.util.List;
import java.util.Map;

import com.google.common.util.concurrent.Futures;
import dev.queriva.ingest.IngestConstants;
import dev.queriva.ingest.QuerivaPointIdMapper;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static io.qdrant.client.ValueFactory.value;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for QdrantSearchService with a mocked Qdrant client.
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
class QdrantSearchServiceTest {

    private static final String COLLECTION = "news_radar";

    @Mock
    private QdrantClient qdrantClient;

    private QdrantSearchService qdrantSearchService;

    @BeforeEach
    void setUp() {
        qdrantSearchService = new QdrantSearchService(qdrantClient);
    }

    @Test
    void should_build_language_filter_and_deduplicate_chunks_by_document_id() {
        when(qdrantClient.searchAsync(any(Points.SearchPoints.class), any()))
                .thenReturn(Futures.immediateFuture(List.of(
                        scoredPoint("cluster-001-chunk-1", "cluster-001", 0.95f, "bn"),
                        scoredPoint("cluster-001-chunk-2", "cluster-001", 0.90f, "bn"))));

        SearchFilters filters = new SearchFilters("bn", null, null, null);

        List<SearchHit> hits = qdrantSearchService.search(
                COLLECTION,
                new float[] {0.1f, 0.2f},
                10,
                0.60,
                filters);

        ArgumentCaptor<Points.SearchPoints> searchCaptor = ArgumentCaptor.forClass(Points.SearchPoints.class);
        verify(qdrantClient).searchAsync(searchCaptor.capture(), any());
        assertThat(searchCaptor.getValue().getFilter().getMustList()).hasSize(1);
        assertThat(searchCaptor.getValue().getFilter().getMustNotList()).hasSize(1);

        assertThat(hits).hasSize(1);
        assertThat(hits.getFirst().id()).isEqualTo("cluster-001");
        assertThat(hits.getFirst().language()).isEqualTo("bn");
    }

    @Test
    void should_filter_out_results_below_min_score_threshold() {
        when(qdrantClient.searchAsync(any(Points.SearchPoints.class), any()))
                .thenReturn(Futures.immediateFuture(List.of(
                        scoredPoint("cluster-001-chunk-1", "cluster-001", 0.55f, "bn"),
                        scoredPoint("cluster-002-chunk-1", "cluster-002", 0.95f, "en"))));

        List<SearchHit> hits = qdrantSearchService.search(
                COLLECTION,
                new float[] {0.1f, 0.2f},
                10,
                0.60,
                null);

        assertThat(hits).hasSize(1);
        assertThat(hits.getFirst().id()).isEqualTo("cluster-002");
    }

    @Test
    void should_map_qdrant_payload_fields_to_search_hit() {
        when(qdrantClient.searchAsync(any(Points.SearchPoints.class), any()))
                .thenReturn(Futures.immediateFuture(List.of(
                        scoredPoint("cluster-003-chunk-1", "cluster-003", 0.88f, "en"))));

        List<SearchHit> hits = qdrantSearchService.search(
                COLLECTION,
                new float[] {0.1f, 0.2f},
                1,
                0.60,
                null);

        assertThat(hits).hasSize(1);
        assertThat(hits.getFirst().title()).isEqualTo("WASA pumping stations overwhelmed");
        assertThat(hits.getFirst().snippet()).isEqualTo("Dhaka WASA officials confirmed drainage strain.");
        assertThat(hits.getFirst().source()).isEqualTo("bdnews24.com");
        assertThat(hits.getFirst().publishedAt()).isEqualTo("2026-06-16T14:00:00Z");
        assertThat(hits.getFirst().url()).isEqualTo("https://example.com/cluster-003");
    }

    private static Points.ScoredPoint scoredPoint(
            String pointId,
            String documentId,
            float score,
            String language) {
        Map<String, String> payloads = Map.of(
                IngestConstants.PAYLOAD_DOCUMENT_ID, documentId,
                IngestConstants.PAYLOAD_TITLE, titleFor(documentId),
                IngestConstants.PAYLOAD_BODY_SNIPPET, snippetFor(documentId),
                IngestConstants.PAYLOAD_SOURCE, sourceFor(documentId),
                IngestConstants.PAYLOAD_LANGUAGE, language,
                IngestConstants.PAYLOAD_PUBLISHED_AT, publishedAtFor(documentId),
                IngestConstants.PAYLOAD_URL, urlFor(documentId));

        Points.ScoredPoint.Builder builder = Points.ScoredPoint.newBuilder()
                .setId(QuerivaPointIdMapper.toQdrantPointId(pointId))
                .setScore(score);

        payloads.forEach((key, payloadValue) -> builder.putPayload(key, value(payloadValue)));
        return builder.build();
    }

    private static String titleFor(String documentId) {
        return switch (documentId) {
            case "cluster-001" -> "Buriganga river overflows";
            case "cluster-002" -> "200,000 residents displaced";
            case "cluster-003" -> "WASA pumping stations overwhelmed";
            default -> "Flood article";
        };
    }

    private static String snippetFor(String documentId) {
        return switch (documentId) {
            case "cluster-003" -> "Dhaka WASA officials confirmed drainage strain.";
            default -> "Heavy monsoon rains affected Dhaka.";
        };
    }

    private static String sourceFor(String documentId) {
        return switch (documentId) {
            case "cluster-003" -> "bdnews24.com";
            default -> "prothomalo.com";
        };
    }

    private static String publishedAtFor(String documentId) {
        return switch (documentId) {
            case "cluster-003" -> "2026-06-16T14:00:00Z";
            default -> "2026-06-15T08:30:00Z";
        };
    }

    private static String urlFor(String documentId) {
        return "https://example.com/" + documentId;
    }
}
