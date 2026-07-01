package dev.queriva.health;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// SPEC.md §6 — GET /api/health
@RestController
@RequestMapping("/api")
public class HealthController {

    private final DependencyHealthService dependencyHealthService;

    /**
     * Creates the health controller with the dependency probe service.
     */
    public HealthController(DependencyHealthService dependencyHealthService) {
        this.dependencyHealthService = dependencyHealthService;
    }

    /**
     * Returns live connectivity status for Qdrant, Ollama, and embed-sidecar.
     */
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(dependencyHealthService.checkHealth());
    }
}
