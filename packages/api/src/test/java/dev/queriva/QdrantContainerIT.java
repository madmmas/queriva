package dev.queriva;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies Testcontainers can start a Qdrant instance for integration tests.
 */
@Testcontainers
@Tag("integration")
class QdrantContainerIT {

    private static final DockerImageName QDRANT_IMAGE = DockerImageName.parse("qdrant/qdrant:latest");

    @Container
    static GenericContainer<?> qdrant = new GenericContainer<>(QDRANT_IMAGE)
            .withExposedPorts(6333, 6334);

    @Test
    void should_respond_on_rest_port_when_qdrant_container_starts() throws Exception {
        String baseUrl = "http://" + qdrant.getHost() + ":" + qdrant.getMappedPort(6333);
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("title");
    }
}
