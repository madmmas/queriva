package dev.queriva;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import dev.queriva.support.QdrantTestcontainersSupport;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies Testcontainers Qdrant config matches docker-compose and SPEC §12.
 */
@Testcontainers
@Tag("integration")
class QdrantContainerIT {

    @Container
    static GenericContainer<?> qdrant = QdrantTestcontainersSupport.newQdrantContainer();

    @Test
    void should_respond_on_rest_port_when_qdrant_container_starts() throws Exception {
        String baseUrl = QdrantTestcontainersSupport.restBaseUrl(qdrant);
        HttpResponse<String> response = httpGet(baseUrl);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("title");
    }

    @Test
    void should_return_ok_from_healthz_endpoint() throws Exception {
        String healthUrl = QdrantTestcontainersSupport.restBaseUrl(qdrant) + "/healthz";
        HttpResponse<String> response = httpGet(healthUrl);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("healthz check passed");
    }

    @Test
    void should_return_collections_list_from_rest_api() throws Exception {
        String collectionsUrl = QdrantTestcontainersSupport.restBaseUrl(qdrant) + "/collections";
        HttpResponse<String> response = httpGet(collectionsUrl);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"collections\"");
        assertThat(response.body()).contains("\"status\":\"ok\"");
    }

    private static HttpResponse<String> httpGet(String url) throws Exception {
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
