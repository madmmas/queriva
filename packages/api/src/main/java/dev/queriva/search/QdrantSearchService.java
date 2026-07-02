package dev.queriva.search;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import dev.queriva.common.QdrantOperationException;
import dev.queriva.ingest.IngestConstants;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points;
import org.springframework.stereotype.Service;

import static io.qdrant.client.WithPayloadSelectorFactory.enable;

/**
 * Vector search against Qdrant with typed filters and document-level deduplication (SPEC §8 step 3, ADR-001).
 */
@Service
public class QdrantSearchService {

    private final QdrantClient qdrantClient;

    /**
     * Creates the search service with a shared Qdrant client bean.
     */
    public QdrantSearchService(QdrantClient qdrantClient) {
        this.qdrantClient = qdrantClient;
    }

    /**
     * Searches a collection and returns ranked, deduplicated hits above the minimum score threshold.
     */
    public List<SearchHit> search(
            String collection,
            float[] vector,
            int topK,
            double minScore,
            SearchFilters filters) {
        Points.SearchPoints searchRequest = Points.SearchPoints.newBuilder()
                .setCollectionName(collection)
                .addAllVector(toFloatList(vector))
                .setFilter(QdrantSearchFilterBuilder.buildFilter(filters))
                .setLimit(computeFetchLimit(topK))
                .setWithPayload(enable(true))
                .build();

        List<Points.ScoredPoint> scoredPoints = executeSearch(searchRequest);
        return deduplicateAndMap(scoredPoints, topK, minScore);
    }

    private List<Points.ScoredPoint> executeSearch(Points.SearchPoints searchRequest) {
        try {
            return qdrantClient.searchAsync(searchRequest, SearchConstants.SEARCH_OPERATION_TIMEOUT)
                    .get(
                            SearchConstants.SEARCH_OPERATION_TIMEOUT.toMillis(),
                            TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new QdrantOperationException(
                    "Qdrant search interrupted for collection '" + searchRequest.getCollectionName()
                            + "'. Retry the request.",
                    exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new QdrantOperationException(
                    "Qdrant search failed for collection '" + searchRequest.getCollectionName() + "'. "
                            + "Verify Qdrant is running at QDRANT_URL.",
                    exception);
        }
    }

    private static long computeFetchLimit(int topK) {
        return (long) topK * SearchConstants.SEARCH_OVERFETCH_MULTIPLIER;
    }

    private static List<Float> toFloatList(float[] vector) {
        List<Float> values = new ArrayList<>(vector.length);
        for (float value : vector) {
            values.add(value);
        }
        return values;
    }

    private static List<SearchHit> deduplicateAndMap(
            List<Points.ScoredPoint> scoredPoints,
            int topK,
            double minScore) {
        List<SearchHit> hits = new ArrayList<>();
        Set<String> seenDocumentIds = new HashSet<>();

        for (Points.ScoredPoint scoredPoint : scoredPoints) {
            if (scoredPoint.getScore() < minScore) {
                continue;
            }

            Map<String, JsonWithInt.Value> payload = scoredPoint.getPayloadMap();
            if (isInternalMetadataPoint(payload)) {
                continue;
            }

            String documentId = readStringPayload(payload, IngestConstants.PAYLOAD_DOCUMENT_ID);
            if (documentId == null || !seenDocumentIds.add(documentId)) {
                continue;
            }

            hits.add(mapSearchHit(scoredPoint, documentId, payload));
            if (hits.size() >= topK) {
                break;
            }
        }

        return hits;
    }

    private static boolean isInternalMetadataPoint(Map<String, JsonWithInt.Value> payload) {
        JsonWithInt.Value internalFlag = payload.get(IngestConstants.PAYLOAD_QUERIVA_INTERNAL);
        return internalFlag != null && internalFlag.getBoolValue();
    }

    private static SearchHit mapSearchHit(
            Points.ScoredPoint scoredPoint,
            String documentId,
            Map<String, JsonWithInt.Value> payload) {
        return new SearchHit(
                documentId,
                scoredPoint.getScore(),
                readStringPayload(payload, IngestConstants.PAYLOAD_TITLE),
                readStringPayload(payload, IngestConstants.PAYLOAD_BODY_SNIPPET),
                readStringPayload(payload, IngestConstants.PAYLOAD_SOURCE),
                readStringPayload(payload, IngestConstants.PAYLOAD_LANGUAGE),
                readStringPayload(payload, IngestConstants.PAYLOAD_PUBLISHED_AT),
                readStringPayload(payload, IngestConstants.PAYLOAD_URL));
    }

    private static String readStringPayload(Map<String, JsonWithInt.Value> payload, String fieldName) {
        JsonWithInt.Value value = payload.get(fieldName);
        if (value == null || !value.hasStringValue()) {
            return null;
        }

        String text = value.getStringValue();
        return text.isBlank() ? null : text;
    }
}
