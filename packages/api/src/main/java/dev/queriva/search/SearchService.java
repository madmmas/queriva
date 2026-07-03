package dev.queriva.search;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import dev.queriva.ingest.CollectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Orchestrates embed → search → optional synthesis for POST /api/search (SPEC §8).
 */
@Service
public class SearchService {

    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);

    private final CollectionManager collectionManager;
    private final QueryEmbeddingService queryEmbeddingService;
    private final QdrantSearchService qdrantSearchService;
    private final LLMSynthesisService llmSynthesisService;
    private final SearchResultMapper searchResultMapper;
    private final double maxScoreAutoAccept;

    /**
     * Creates the search orchestrator with pipeline dependencies.
     */
    public SearchService(
            CollectionManager collectionManager,
            QueryEmbeddingService queryEmbeddingService,
            QdrantSearchService qdrantSearchService,
            LLMSynthesisService llmSynthesisService,
            SearchResultMapper searchResultMapper,
            @Value("${search.max-score-auto-accept}") double maxScoreAutoAccept) {
        this.collectionManager = collectionManager;
        this.queryEmbeddingService = queryEmbeddingService;
        this.qdrantSearchService = qdrantSearchService;
        this.llmSynthesisService = llmSynthesisService;
        this.searchResultMapper = searchResultMapper;
        this.maxScoreAutoAccept = maxScoreAutoAccept;
    }

    /**
     * Runs the search pipeline for the given request and returns a SPEC §6 response.
     */
    public SearchResponse search(SearchRequest request) {
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

        if (SearchModes.SEARCH.equals(request.mode())) {
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

        return executeRagMode(
                request,
                hits,
                embedLatencyMs,
                searchLatencyMs,
                totalStartNanos);
    }

    private SearchResponse executeRagMode(
            SearchRequest request,
            List<SearchHit> hits,
            long embedLatencyMs,
            long searchLatencyMs,
            long totalStartNanos) {
        Long synthesisLatencyMs = null;
        String summary = null;

        if (shouldSynthesize(hits)) {
            long synthesisStartNanos = System.nanoTime();
            Optional<String> synthesis = llmSynthesisService.synthesize(request.query(), hits);
            synthesisLatencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - synthesisStartNanos);
            summary = synthesis.orElse(null);
        }

        long totalLatencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - totalStartNanos);

        logger.info(
                "RAG search completed for collection '{}' ({} results, summary={}, {} ms total)",
                request.collection(),
                hits.size(),
                summary != null ? "present" : "absent",
                totalLatencyMs);
        logger.debug("Query text: {}", request.query());

        return searchResultMapper.toRagResponse(
                request,
                hits,
                embedLatencyMs,
                searchLatencyMs,
                synthesisLatencyMs,
                summary,
                totalLatencyMs);
    }

    private boolean shouldSynthesize(List<SearchHit> hits) {
        if (hits.isEmpty()) {
            return false;
        }
        return hits.getFirst().score() < maxScoreAutoAccept;
    }
}
