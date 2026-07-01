package com.merge.backend.curriculum.repository;

import com.merge.backend.curriculum.domain.Concept;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConceptRepository extends JpaRepository<Concept, Long> {
    List<Concept> findByStageNameOrderBySequenceOrder(String stageName);
}
