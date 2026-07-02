package dev.queriva.search;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// SPEC.md §6 — POST /api/search
@RestController
@RequestMapping("/api")
public class SearchController {

    private final SearchService searchService;

    /**
     * Creates the search controller with the search orchestration service.
     */
    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * Semantic search against a Qdrant collection (search or RAG mode).
     */
    @PostMapping("/search")
    public ResponseEntity<SearchResponse> search(@Valid @RequestBody SearchRequest request) {
        return ResponseEntity.ok(searchService.search(request));
    }
}
