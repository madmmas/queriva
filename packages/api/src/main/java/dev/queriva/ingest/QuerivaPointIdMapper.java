package dev.queriva.ingest;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import io.qdrant.client.grpc.Points;

import static io.qdrant.client.PointIdFactory.id;

/**
 * Maps logical Queriva point ids (e.g. {@code cluster-001-chunk-1}) to Qdrant point ids.
 */
public final class QuerivaPointIdMapper {

    private QuerivaPointIdMapper() {
    }

    /**
     * Converts a logical point id string to a deterministic Qdrant UUID point id (ADR-008).
     */
    public static Points.PointId toQdrantPointId(String logicalPointId) {
        UUID pointUuid = UUID.nameUUIDFromBytes(logicalPointId.getBytes(StandardCharsets.UTF_8));
        return id(pointUuid);
    }
}
