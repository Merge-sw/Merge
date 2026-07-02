package com.merge.backend.curriculum.controller;

import com.merge.backend.curriculum.dto.ConceptResourceResponse;
import com.merge.backend.curriculum.repository.ConceptRepository;
import com.merge.backend.curriculum.service.ConceptNotFoundException;
import com.merge.backend.curriculum.repository.ConceptResourceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/concepts")
public class ConceptResourceController {

    private final ConceptRepository conceptRepository;
    private final ConceptResourceRepository resourceRepository;

    public ConceptResourceController(ConceptRepository conceptRepository,
                                     ConceptResourceRepository resourceRepository) {
        this.conceptRepository = conceptRepository;
        this.resourceRepository = resourceRepository;
    }

    /**
     * CU-06: GET /api/v1/concepts/{id}/resources
     * Returns all supplementary resources for a concept. Resources are never
     * required to pass a Drill — they are optional study aids.
     */
    @GetMapping("/{id}/resources")
    public ResponseEntity<List<ConceptResourceResponse>> getResources(@PathVariable Long id) {
        if (!conceptRepository.existsById(id)) {
            throw new ConceptNotFoundException("Concept not found: " + id);
        }
        List<ConceptResourceResponse> resources = resourceRepository
                .findByConceptIdOrderByTypeAscTitleAsc(id)
                .stream()
                .map(ConceptResourceResponse::from)
                .toList();
        return ResponseEntity.ok(resources);
    }

    @ExceptionHandler(ConceptNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ConceptNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }
}
