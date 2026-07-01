package dev.queriva.ingest;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import dev.queriva.common.QdrantOperationException;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points;
import org.springframework.stereotype.Repository;

import static io.qdrant.client.ConditionFactory.matchKeyword;
import static io.qdrant.client.ValueFactory.value;
import static io.qdrant.client.VectorsFactory.vectors;

/**
 * Low-level Qdrant point operations for the ingest pipeline (code-quality.mdc E5).
 */
@Repository
public class QdrantIngestRepository {

    private static final Duration OPERATION_TIMEOUT = Duration.ofSeconds(30);

    private final QdrantClient qdrantClient;

    /**
     * Creates the repository with a shared Qdrant client bean.
     */
    public QdrantIngestRepository(QdrantClient qdrantClient) {
        this.qdrantClient = qdrantClient;
    }

    /**
     * Returns true when a point with the given string id exists in the collection.
     */
    public boolean pointExists(String collectionName, String pointId) {
        try {
            List<Points.RetrievedPoint> points = qdrantClient.retrieveAsync(
                            collectionName,
                            List.of(buildStringPointId(pointId)),
                            null)
                    .get(OPERATION_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

            return !points.isEmpty();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new QdrantOperationException(
                    "Qdrant retrieve interrupted for point '" + pointId + "'. Retry the request.",
                    exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new QdrantOperationException(
                    "Qdrant retrieve failed for point '" + pointId + "'. "
                            + "Verify Qdrant is running at QDRANT_URL.",
                    exception);
        }
    }

    /**
     * Deletes all points whose payload document_id matches the given value.
     */
    public void deleteByDocumentId(String collectionName, String documentId) {
        Points.Filter filter = Points.Filter.newBuilder()
                .addMust(matchKeyword(IngestConstants.PAYLOAD_DOCUMENT_ID, documentId))
                .build();

        try {
            qdrantClient.deleteAsync(collectionName, filter, OPERATION_TIMEOUT)
                    .get(OPERATION_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new QdrantOperationException(
                    "Qdrant delete interrupted for document '" + documentId + "'. Retry the request.",
                    exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new QdrantOperationException(
                    "Qdrant delete failed for document '" + documentId + "'. "
                            + "Verify Qdrant is running at QDRANT_URL.",
                    exception);
        }
    }

    /**
     * Upserts the given points into the target collection.
     */
    public void upsertPoints(String collectionName, List<Points.PointStruct> points) {
        if (points.isEmpty()) {
            return;
        }

        try {
            qdrantClient.upsertAsync(collectionName, points, OPERATION_TIMEOUT)
                    .get(OPERATION_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new QdrantOperationException(
                    "Qdrant upsert interrupted for collection '" + collectionName + "'. Retry the request.",
                    exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new QdrantOperationException(
                    "Qdrant upsert failed for collection '" + collectionName + "'. "
                            + "Verify Qdrant is running at QDRANT_URL.",
                    exception);
        }
    }

    /**
     * Reads the embedding model stored in the collection metadata point, if present.
     */
    public Optional<String> readStoredEmbeddingModel(String collectionName) {
        try {
            List<Points.RetrievedPoint> points = qdrantClient.retrieveAsync(
                            collectionName,
                            List.of(buildStringPointId(IngestConstants.COLLECTION_METADATA_POINT_ID)),
                            true,
                            false,
                            null)
                    .get(OPERATION_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

            if (points.isEmpty()) {
                return Optional.empty();
            }

            JsonWithInt.Value modelValue = points.getFirst()
                    .getPayloadMap()
                    .get(IngestConstants.PAYLOAD_EMBEDDING_MODEL);

            if (modelValue == null || !modelValue.hasStringValue()) {
                return Optional.empty();
            }

            return Optional.of(modelValue.getStringValue());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new QdrantOperationException(
                    "Qdrant metadata read interrupted for collection '" + collectionName + "'. Retry the request.",
                    exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new QdrantOperationException(
                    "Qdrant metadata read failed for collection '" + collectionName + "'. "
                            + "Verify Qdrant is running at QDRANT_URL.",
                    exception);
        }
    }

    /**
     * Stores the embedding model name in the collection metadata point.
     */
    public void writeStoredEmbeddingModel(String collectionName, String modelName, int vectorSize) {
        List<Float> zeroVector = new java.util.ArrayList<>(vectorSize);
        for (int index = 0; index < vectorSize; index++) {
            zeroVector.add(0.0f);
        }

        Points.PointStruct metadataPoint = Points.PointStruct.newBuilder()
                .setId(buildStringPointId(IngestConstants.COLLECTION_METADATA_POINT_ID))
                .setVectors(vectors(zeroVector))
                .putAllPayload(Map.of(
                        IngestConstants.PAYLOAD_EMBEDDING_MODEL, value(modelName),
                        IngestConstants.PAYLOAD_QUERIVA_INTERNAL, value(true)))
                .build();

        upsertPoints(collectionName, List.of(metadataPoint));
    }

    /**
     * Builds a Qdrant point struct for one ingested chunk.
     */
    public Points.PointStruct buildPoint(
            Chunk chunk,
            IngestDocumentDto document,
            List<Float> vector) {
        Points.PointStruct.Builder builder = Points.PointStruct.newBuilder()
                .setId(buildStringPointId(chunk.pointId()))
                .setVectors(vectors(vector))
                .putPayload(IngestConstants.PAYLOAD_TITLE, value(document.title()))
                .putPayload(IngestConstants.PAYLOAD_BODY_SNIPPET, value(chunk.bodySnippet()))
                .putPayload(IngestConstants.PAYLOAD_DOCUMENT_ID, value(chunk.documentId()));

        putOptionalPayload(builder, IngestConstants.PAYLOAD_SOURCE, document.source());
        putOptionalPayload(builder, IngestConstants.PAYLOAD_LANGUAGE, document.language());
        putOptionalPayload(builder, IngestConstants.PAYLOAD_PUBLISHED_AT, document.publishedAt());
        putOptionalPayload(builder, IngestConstants.PAYLOAD_CATEGORY, document.category());
        putOptionalPayload(builder, IngestConstants.PAYLOAD_URL, document.url());
        putOptionalPayload(builder, IngestConstants.PAYLOAD_CLUSTER_ID, document.clusterId());

        return builder.build();
    }

    private static void putOptionalPayload(
            Points.PointStruct.Builder builder,
            String key,
            String payloadValue) {
        if (payloadValue != null && !payloadValue.isBlank()) {
            builder.putPayload(key, value(payloadValue));
        }
    }

    private static Points.PointId buildStringPointId(String pointId) {
        return QuerivaPointIdMapper.toQdrantPointId(pointId);
    }
}
