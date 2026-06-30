package dev.queriva.support;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared Testcontainers configuration for Qdrant integration tests (ADR-009).
 */
public final class QdrantTestcontainersSupport {

    /** Official Qdrant image — must match docker-compose.yml and SPEC §12. */
    public static final DockerImageName QDRANT_IMAGE =
            DockerImageName.parse("qdrant/qdrant:latest");

    /** REST API port exposed by qdrant/qdrant (collections, healthz). */
    public static final int REST_PORT = 6333;

    /** gRPC port exposed by qdrant/qdrant (Java client). */
    public static final int GRPC_PORT = 6334;

    private QdrantTestcontainersSupport() {
    }

    /**
     * Returns a Qdrant container with REST and gRPC ports exposed, matching compose config.
     */
    public static GenericContainer<?> newQdrantContainer() {
        return new GenericContainer<>(QDRANT_IMAGE)
                .withExposedPorts(REST_PORT, GRPC_PORT);
    }

    /**
     * Builds the REST base URL for a running Testcontainers Qdrant instance.
     */
    public static String restBaseUrl(GenericContainer<?> qdrantContainer) {
        return "http://" + qdrantContainer.getHost() + ":" + qdrantContainer.getMappedPort(REST_PORT);
    }
}
