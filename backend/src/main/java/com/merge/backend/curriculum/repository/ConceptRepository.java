package com.merge.backend.curriculum.repository;

import com.merge.backend.curriculum.domain.Concept;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConceptRepository extends JpaRepository<Concept, Long> {
    List<Concept> findByStageNameOrderBySequenceOrder(String stageName);
    Optional<Concept> findByStageNameAndSequenceOrder(String stageName, int sequenceOrder);
    Optional<Concept> findFirstByStageNameOrderBySequenceOrderAsc(String stageName);
    long countByStageName(String stageName);
}
