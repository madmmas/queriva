package dev.queriva.ingest;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import dev.queriva.common.CollectionAlreadyExistsException;
import dev.queriva.common.CollectionNotFoundException;
import dev.queriva.common.QdrantOperationException;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections;
import org.springframework.stereotype.Service;

/**
 * Qdrant collection lifecycle operations used by the ingest API (ADR-011).
 */
@Service
public class CollectionManager {

    private static final Duration OPERATION_TIMEOUT = Duration.ofSeconds(30);

    private final QdrantClient qdrantClient;

    /**
     * Creates the collection manager with a shared Qdrant client bean.
     */
    public CollectionManager(QdrantClient qdrantClient) {
        this.qdrantClient = qdrantClient;
    }

    /**
     * Creates or recreates a Qdrant collection with the given vector configuration.
     */
    public CollectionSummary createCollection(CreateCollectionRequest request) {
        String collectionName = request.collection();
        Collections.VectorParams vectorParams = buildVectorParams(request.vectorSize(), request.distance());

        try {
            boolean exists = qdrantClient.collectionExistsAsync(collectionName, OPERATION_TIMEOUT)
                    .get(OPERATION_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

            if (exists && !request.recreateIfExists()) {
                throw new CollectionAlreadyExistsException(collectionName);
            }

            if (exists) {
                qdrantClient.recreateCollectionAsync(collectionName, vectorParams, OPERATION_TIMEOUT)
                        .get(OPERATION_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            } else {
                qdrantClient.createCollectionAsync(collectionName, vectorParams, OPERATION_TIMEOUT)
                        .get(OPERATION_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            }

            return fetchCollectionSummary(collectionName);
        } catch (CollectionAlreadyExistsException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new QdrantOperationException(
                    "Qdrant create collection interrupted for '" + collectionName + "'. Retry the request.",
                    exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new QdrantOperationException(
                    "Qdrant create collection failed for '" + collectionName + "'. "
                            + "Verify Qdrant is running at QDRANT_URL.",
                    exception);
        }
    }

    /**
     * Lists all collections with vector size, distance metric, and point counts.
     */
    public CollectionListResponse listCollections() {
        try {
            List<String> collectionNames = qdrantClient.listCollectionsAsync(OPERATION_TIMEOUT)
                    .get(OPERATION_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

            List<CollectionSummary> summaries = new ArrayList<>();
            for (String collectionName : collectionNames) {
                summaries.add(fetchCollectionSummary(collectionName));
            }

            return new CollectionListResponse(summaries);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new QdrantOperationException(
                    "Qdrant list collections interrupted. Retry the request.", exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new QdrantOperationException(
                    "Qdrant list collections failed. Verify Qdrant is running at QDRANT_URL.",
                    exception);
        }
    }

    /**
     * Deletes a collection by name when it exists.
     */
    public void deleteCollection(String collectionName) {
        try {
            requireCollectionExists(collectionName);

            qdrantClient.deleteCollectionAsync(collectionName, OPERATION_TIMEOUT)
                    .get(OPERATION_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (CollectionNotFoundException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new QdrantOperationException(
                    "Qdrant delete collection interrupted for '" + collectionName + "'. Retry the request.",
                    exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new QdrantOperationException(
                    "Qdrant delete collection failed for '" + collectionName + "'. "
                            + "Verify Qdrant is running at QDRANT_URL.",
                    exception);
        }
    }

    /**
     * Verifies that a Qdrant collection exists before search or delete operations.
     */
    public void requireCollectionExists(String collectionName) {
        try {
            boolean exists = qdrantClient.collectionExistsAsync(collectionName, OPERATION_TIMEOUT)
                    .get(OPERATION_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

            if (!exists) {
                throw new CollectionNotFoundException(collectionName);
            }
        } catch (CollectionNotFoundException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new QdrantOperationException(
                    "Qdrant collection lookup interrupted for '" + collectionName + "'. Retry the request.",
                    exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new QdrantOperationException(
                    "Qdrant collection lookup failed for '" + collectionName + "'. "
                            + "Verify Qdrant is running at QDRANT_URL.",
                    exception);
        }
    }

    private CollectionSummary fetchCollectionSummary(String collectionName)
            throws InterruptedException, ExecutionException, TimeoutException {
        Collections.CollectionInfo info = qdrantClient.getCollectionInfoAsync(collectionName, OPERATION_TIMEOUT)
                .get(OPERATION_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

        Collections.VectorParams vectorParams = info.getConfig().getParams().getVectorsConfig().getParams();
        return new CollectionSummary(
                collectionName,
                (int) vectorParams.getSize(),
                vectorParams.getDistance().name(),
                info.getPointsCount());
    }

    private Collections.VectorParams buildVectorParams(int vectorSize, String distanceName) {
        return Collections.VectorParams.newBuilder()
                .setSize(vectorSize)
                .setDistance(parseDistance(distanceName))
                .build();
    }

    private Collections.Distance parseDistance(String distanceName) {
        return switch (distanceName.trim()) {
            case "Cosine" -> Collections.Distance.Cosine;
            case "Euclid" -> Collections.Distance.Euclid;
            case "Dot" -> Collections.Distance.Dot;
            case "Manhattan" -> Collections.Distance.Manhattan;
            default -> throw new InvalidDistanceException(distanceName);
        };
    }
}
