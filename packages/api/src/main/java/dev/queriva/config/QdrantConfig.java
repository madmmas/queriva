package dev.queriva.config;

import java.net.URI;
import java.time.Duration;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Configures the Qdrant gRPC client from SPEC §13 environment variables.
 */
@Configuration
public class QdrantConfig {

    private static final int DEFAULT_REST_PORT = 6333;

    private final String qdrantUrl;
    private final String apiKey;
    private final int grpcPort;
    private final int timeoutSeconds;

    /**
     * Creates Qdrant configuration from application properties.
     */
    public QdrantConfig(
            @Value("${qdrant.url}") String qdrantUrl,
            @Value("${qdrant.api-key:}") String apiKey,
            @Value("${qdrant.grpc-port}") int grpcPort,
            @Value("${qdrant.timeout-seconds}") int timeoutSeconds) {
        this.qdrantUrl = qdrantUrl;
        this.apiKey = apiKey;
        this.grpcPort = grpcPort;
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * Provides a shared QdrantClient bean for collection and search operations.
     */
    @Bean(destroyMethod = "close")
    public QdrantClient qdrantClient() {
        URI uri = URI.create(qdrantUrl);
        String host = uri.getHost();
        int port = resolveGrpcPort(uri);

        QdrantGrpcClient.Builder builder = QdrantGrpcClient.newBuilder(host, port, false, false)
                .withTimeout(Duration.ofSeconds(timeoutSeconds));

        if (StringUtils.hasText(apiKey)) {
            builder.withApiKey(apiKey);
        }

        return new QdrantClient(builder.build());
    }

    private int resolveGrpcPort(URI uri) {
        if (uri.getPort() > 0 && uri.getPort() != DEFAULT_REST_PORT) {
            return uri.getPort();
        }
        return grpcPort;
    }
}
