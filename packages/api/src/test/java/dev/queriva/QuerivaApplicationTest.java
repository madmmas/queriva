package dev.queriva;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifies the Spring application context loads with default configuration.
 */
@SpringBootTest
@Tag("unit")
class QuerivaApplicationTest {

    @Test
    void should_load_application_context_when_started() {
        // context load is the assertion — failure means misconfigured beans
    }
}
