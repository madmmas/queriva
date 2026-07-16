package dev.queriva.search;

import java.util.List;
import java.util.Optional;

import dev.queriva.common.CollectionNotFoundException;
import dev.queriva.ingest.CollectionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for search orchestration with mocked pipeline dependencies.
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
class SearchServiceTest {

    private static final String COLLECTION = "news_radar";
    private static final String QUERY = "floods in Dhaka last week";
    private static final float[] QUERY_VECTOR = new float[] {0.1f, 0.2f};
    private static final double MAX_SCORE_AUTO_ACCEPT = 0.80;
    private static final double DEFAULT_MIN_SCORE = 0.40;

    @Mock
    private CollectionManager collectionManager;

    @Mock
    private QueryEmbeddingService queryEmbeddingService;

    @Mock
    private QdrantSearchService qdrantSearchService;

    @Mock
    private LLMSynthesisService llmSynthesisService;

    @Mock
    private SearchResultMapper searchResultMapper;

    private SearchService searchService;

    @BeforeEach
    void setUp() {
        searchService = new SearchService(
                collectionManager,
                queryEmbeddingService,
                qdrantSearchService,
                llmSynthesisService,
                searchResultMapper,
                DEFAULT_MIN_SCORE,
                MAX_SCORE_AUTO_ACCEPT);
    }

    @Test
    void should_orchestrate_embed_search_and_mapping_in_search_mode() {
        SearchRequest request = new SearchRequest(QUERY, COLLECTION, 10, 0.60, SearchModes.SEARCH, null);
        List<SearchHit> hits = sampleHits(0.72);
        SearchResponse mappedResponse = new SearchResponse(
                QUERY, SearchModes.SEARCH, null, hits, new SearchLatencyMs(1, 2, null, 4));

        when(queryEmbeddingService.resolveModel(null)).thenReturn("LaBSE");
        when(queryEmbeddingService.embed(QUERY, "LaBSE")).thenReturn(QUERY_VECTOR);
        when(qdrantSearchService.search(COLLECTION, QUERY_VECTOR, 10, 0.60, null)).thenReturn(hits);
        when(searchResultMapper.toSearchResponse(eq(request), eq(hits), anyLong(), anyLong(), anyLong()))
                .thenReturn(mappedResponse);

        SearchResponse response = searchService.search(request);

        assertThat(response.summary()).isNull();
        assertThat(response.results()).hasSize(1);
        verify(llmSynthesisService, never()).synthesize(any(), any());
    }

    @Test
    void should_synthesize_summary_in_rag_mode_when_top_score_is_below_auto_accept_threshold() {
        SearchRequest request = new SearchRequest(QUERY, COLLECTION, 10, 0.60, SearchModes.RAG, null);
        List<SearchHit> hits = sampleHits(0.72);
        SearchResponse mappedResponse = new SearchResponse(
                QUERY,
                SearchModes.RAG,
                "Summary citing Buriganga floods.",
                hits,
                new SearchLatencyMs(1, 2, 50L, 55));

        when(queryEmbeddingService.resolveModel(null)).thenReturn("LaBSE");
        when(queryEmbeddingService.embed(QUERY, "LaBSE")).thenReturn(QUERY_VECTOR);
        when(qdrantSearchService.search(COLLECTION, QUERY_VECTOR, 10, 0.60, null)).thenReturn(hits);
        when(llmSynthesisService.synthesize(QUERY, hits))
                .thenReturn(Optional.of("Summary citing Buriganga floods."));
        when(searchResultMapper.toRagResponse(
                        eq(request), eq(hits), anyLong(), anyLong(), anyLong(), eq("Summary citing Buriganga floods."), anyLong()))
                .thenReturn(mappedResponse);

        SearchResponse response = searchService.search(request);

        assertThat(response.mode()).isEqualTo(SearchModes.RAG);
        assertThat(response.summary()).contains("Buriganga");
        assertThat(response.latencyMs().synthesis()).isPositive();
        verify(llmSynthesisService).synthesize(QUERY, hits);
    }

    @Test
    void should_skip_synthesis_when_top_score_meets_auto_accept_threshold() {
        SearchRequest request = new SearchRequest(QUERY, COLLECTION, 10, 0.60, SearchModes.RAG, null);
        List<SearchHit> hits = sampleHits(0.95);
        SearchResponse mappedResponse = new SearchResponse(
                QUERY, SearchModes.RAG, null, hits, new SearchLatencyMs(1, 2, null, 4));

        when(queryEmbeddingService.resolveModel(null)).thenReturn("LaBSE");
        when(queryEmbeddingService.embed(QUERY, "LaBSE")).thenReturn(QUERY_VECTOR);
        when(qdrantSearchService.search(COLLECTION, QUERY_VECTOR, 10, 0.60, null)).thenReturn(hits);
        when(searchResultMapper.toRagResponse(
                        eq(request), eq(hits), anyLong(), anyLong(), eq(null), eq(null), anyLong()))
                .thenReturn(mappedResponse);

        SearchResponse response = searchService.search(request);

        assertThat(response.summary()).isNull();
        verify(llmSynthesisService, never()).synthesize(any(), any());
    }

    @Test
    void should_degrade_gracefully_when_ollama_synthesis_returns_empty_in_rag_mode() {
        SearchRequest request = new SearchRequest(QUERY, COLLECTION, 10, 0.60, SearchModes.RAG, null);
        List<SearchHit> hits = sampleHits(0.72);
        SearchResponse mappedResponse = new SearchResponse(
                QUERY, SearchModes.RAG, null, hits, new SearchLatencyMs(1, 2, 10L, 14));

        when(queryEmbeddingService.resolveModel(null)).thenReturn("LaBSE");
        when(queryEmbeddingService.embed(QUERY, "LaBSE")).thenReturn(QUERY_VECTOR);
        when(qdrantSearchService.search(COLLECTION, QUERY_VECTOR, 10, 0.60, null)).thenReturn(hits);
        when(llmSynthesisService.synthesize(QUERY, hits)).thenReturn(Optional.empty());
        when(searchResultMapper.toRagResponse(
                        eq(request), eq(hits), anyLong(), anyLong(), anyLong(), eq(null), anyLong()))
                .thenReturn(mappedResponse);

        SearchResponse response = searchService.search(request);

        assertThat(response.summary()).isNull();
        assertThat(response.results()).hasSize(1);
    }

    @Test
    void should_return_different_response_shapes_for_search_mode_versus_rag_mode() {
        List<SearchHit> hits = sampleHits(0.72);

        SearchResponse searchModeResponse = new SearchResponse(
                QUERY, SearchModes.SEARCH, null, hits, new SearchLatencyMs(1, 2, null, 4));
        SearchResponse ragModeResponse = new SearchResponse(
                QUERY,
                SearchModes.RAG,
                "AI summary",
                hits,
                new SearchLatencyMs(1, 2, 20L, 24));

        when(queryEmbeddingService.resolveModel(null)).thenReturn("LaBSE");
        when(queryEmbeddingService.embed(QUERY, "LaBSE")).thenReturn(QUERY_VECTOR);
        when(qdrantSearchService.search(COLLECTION, QUERY_VECTOR, 10, 0.60, null)).thenReturn(hits);
        when(searchResultMapper.toSearchResponse(any(), eq(hits), anyLong(), anyLong(), anyLong()))
                .thenReturn(searchModeResponse);
        when(llmSynthesisService.synthesize(QUERY, hits)).thenReturn(Optional.of("AI summary"));
        when(searchResultMapper.toRagResponse(
                        any(), eq(hits), anyLong(), anyLong(), anyLong(), eq("AI summary"), anyLong()))
                .thenReturn(ragModeResponse);

        SearchRequest searchRequest = new SearchRequest(QUERY, COLLECTION, 10, 0.60, SearchModes.SEARCH, null);
        SearchRequest ragRequest = new SearchRequest(QUERY, COLLECTION, 10, 0.60, SearchModes.RAG, null);

        SearchResponse searchResult = searchService.search(searchRequest);
        SearchResponse ragResult = searchService.search(ragRequest);

        assertThat(searchResult.summary()).isNull();
        assertThat(searchResult.latencyMs().synthesis()).isNull();
        assertThat(ragResult.summary()).isNotBlank();
        assertThat(ragResult.latencyMs().synthesis()).isPositive();
    }

    @Test
    void should_throw_when_collection_does_not_exist() {
        SearchRequest request = new SearchRequest(QUERY, COLLECTION, 10, 0.60, SearchModes.SEARCH, null);
        doThrow(new CollectionNotFoundException(COLLECTION))
                .when(collectionManager)
                .requireCollectionExists(COLLECTION);

        assertThatThrownBy(() -> searchService.search(request))
                .isInstanceOf(CollectionNotFoundException.class);
    }

    @Test
    void should_use_configured_min_score_when_request_omits_min_score() {
        SearchRequest request = new SearchRequest(QUERY, COLLECTION, 10, null, SearchModes.SEARCH, null);
        List<SearchHit> hits = sampleHits(0.45);
        SearchResponse mappedResponse = new SearchResponse(
                QUERY, SearchModes.SEARCH, null, hits, new SearchLatencyMs(1, 2, null, 4));

        when(queryEmbeddingService.resolveModel(null)).thenReturn("LaBSE");
        when(queryEmbeddingService.embed(QUERY, "LaBSE")).thenReturn(QUERY_VECTOR);
        when(qdrantSearchService.search(COLLECTION, QUERY_VECTOR, 10, DEFAULT_MIN_SCORE, null)).thenReturn(hits);
        when(searchResultMapper.toSearchResponse(eq(request), eq(hits), anyLong(), anyLong(), anyLong()))
                .thenReturn(mappedResponse);

        SearchResponse response = searchService.search(request);

        assertThat(response.results()).hasSize(1);
        verify(qdrantSearchService).search(COLLECTION, QUERY_VECTOR, 10, DEFAULT_MIN_SCORE, null);
    }

    private static List<SearchHit> sampleHits(double score) {
        return List.of(new SearchHit(
                "cluster-001", score, "Title", "Snippet", "source", "bn", "2026-06-15T08:30:00Z", "https://x"));
    }
}
