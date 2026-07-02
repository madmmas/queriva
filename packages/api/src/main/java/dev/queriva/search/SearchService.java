package dev.queriva.search;

import java.util.List;
import java.util.concurrent.TimeUnit;

import dev.queriva.ingest.CollectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Orchestrates embed → search → response mapping for POST /api/search (SPEC §8).
 */
@Service
public class SearchService {

    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);

    private final CollectionManager collectionManager;
    private final QueryEmbeddingService queryEmbeddingService;
    private final QdrantSearchService qdrantSearchService;
    private final SearchResultMapper searchResultMapper;

    /**
     * Creates the search orchestrator with pipeline dependencies.
     */
    public SearchService(
            CollectionManager collectionManager,
            QueryEmbeddingService queryEmbeddingService,
            QdrantSearchService qdrantSearchService,
            SearchResultMapper searchResultMapper) {
        this.collectionManager = collectionManager;
        this.queryEmbeddingService = queryEmbeddingService;
        this.qdrantSearchService = qdrantSearchService;
        this.searchResultMapper = searchResultMapper;
    }

    /**
     * Runs the search pipeline for the given request and returns a SPEC §6 response.
     */
    public SearchResponse search(SearchRequest request) {
        if (!SearchModes.SEARCH.equals(request.mode())) {
            throw new IllegalArgumentException(
                    "mode='" + request.mode() + "' is not available yet. Use mode='search' until RAG is implemented.");
        }

        long totalStartNanos = System.nanoTime();

        collectionManager.requireCollectionExists(request.collection());

        String embeddingModel = queryEmbeddingService.resolveModel(null);
        queryEmbeddingService.validateEmbeddingModel(request.collection(), embeddingModel);

        long embedStartNanos = System.nanoTime();
        float[] queryVector = queryEmbeddingService.embed(request.query(), embeddingModel);
        long embedLatencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - embedStartNanos);

        long searchStartNanos = System.nanoTime();
        List<SearchHit> hits = qdrantSearchService.search(
                request.collection(),
                queryVector,
                request.topK(),
                request.minScore(),
                request.filters());
        long searchLatencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - searchStartNanos);

        long totalLatencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - totalStartNanos);

        logger.info(
                "Search completed for collection '{}' ({} results, {} ms total)",
                request.collection(),
                hits.size(),
                totalLatencyMs);
        logger.debug("Query text: {}", request.query());

        return searchResultMapper.toSearchResponse(
                request,
                hits,
                embedLatencyMs,
                searchLatencyMs,
                totalLatencyMs);
    }
}
