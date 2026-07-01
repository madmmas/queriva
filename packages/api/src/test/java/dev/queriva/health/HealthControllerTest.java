package dev.queriva.health;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for GET /api/health with mocked dependency probes.
 */
@WebMvcTest(HealthController.class)
@Tag("unit")
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DependencyHealthService dependencyHealthService;

    @Test
    void should_return_ok_status_when_qdrant_and_embed_sidecar_are_connected() throws Exception {
        when(dependencyHealthService.checkHealth()).thenReturn(
                new HealthResponse("ok", "connected", "connected", "connected"));

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.qdrant").value("connected"))
                .andExpect(jsonPath("$.ollama").value("connected"))
                .andExpect(jsonPath("$.embed_sidecar").value("connected"));
    }

    @Test
    void should_return_degraded_status_when_qdrant_is_disconnected() throws Exception {
        when(dependencyHealthService.checkHealth()).thenReturn(
                new HealthResponse("degraded", "disconnected", "connected", "connected"));

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("degraded"))
                .andExpect(jsonPath("$.qdrant").value("disconnected"));
    }
}
