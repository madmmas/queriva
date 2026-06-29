package dev.queriva;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.RestClient;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies WireMock is configured for HTTP client unit tests.
 */
@Tag("unit")
class WireMockSetupTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Test
    void should_return_stubbed_json_when_wire_mock_is_configured() {
        wireMock.stubFor(get(urlEqualTo("/api/health"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"ok\"}")));

        RestClient client = RestClient.builder()
                .baseUrl(wireMock.baseUrl())
                .build();
        String body = client.get()
                .uri("/api/health")
                .retrieve()
                .body(String.class);

        assertThat(body).contains("\"status\":\"ok\"");
        wireMock.verify(getRequestedFor(urlEqualTo("/api/health")));
    }
}
