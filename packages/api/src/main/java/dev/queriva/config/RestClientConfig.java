package dev.queriva.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * RestClient factory with explicit connect and read timeouts (code-quality.mdc B5).
 */
@Configuration
public class RestClientConfig {

    private final int connectTimeoutSeconds;
    private final int readTimeoutSeconds;

    /**
     * Creates HTTP client configuration from SPEC §13 timeout properties.
     */
    public RestClientConfig(
            @Value("${http.connect-timeout-seconds}") int connectTimeoutSeconds,
            @Value("${http.read-timeout-seconds}") int readTimeoutSeconds) {
        this.connectTimeoutSeconds = connectTimeoutSeconds;
        this.readTimeoutSeconds = readTimeoutSeconds;
    }

    /**
     * Provides a RestClient.Builder preconfigured with connect and read timeouts.
     */
    @Bean
    public RestClient.Builder restClientBuilder() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(connectTimeoutSeconds));
        requestFactory.setReadTimeout(Duration.ofSeconds(readTimeoutSeconds));

        return RestClient.builder().requestFactory(requestFactory);
    }
}
