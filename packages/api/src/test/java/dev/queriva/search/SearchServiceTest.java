package dev.queriva.search;

import java.util.List;

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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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

    @Mock
    private CollectionManager collectionManager;

    @Mock
    private QueryEmbeddingService queryEmbeddingService;

    @Mock
    private QdrantSearchService qdrantSearchService;

    @Mock
    private SearchResultMapper searchResultMapper;

    private SearchService searchService;

    @BeforeEach
    void setUp() {
        searchService = new SearchService(
                collectionManager,
                queryEmbeddingService,
                qdrantSearchService,
                searchResultMapper);
    }

    @Test
    void should_orchestrate_embed_search_and_mapping_in_search_mode() {
        SearchRequest request = new SearchRequest(QUERY, COLLECTION, 10, 0.60, SearchModes.SEARCH, null);
        List<SearchHit> hits = List.of(new SearchHit(
                "cluster-001", 0.92, "Title", "Snippet", "source", "bn", "2026-06-15T08:30:00Z", "https://x"));
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
        verify(collectionManager).requireCollectionExists(COLLECTION);
        verify(queryEmbeddingService).validateEmbeddingModel(COLLECTION, "LaBSE");
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
    void should_reject_rag_mode_until_implemented() {
        SearchRequest request = new SearchRequest(QUERY, COLLECTION, 10, 0.60, SearchModes.RAG, null);

        assertThatThrownBy(() -> searchService.search(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not available yet");
    }
}
