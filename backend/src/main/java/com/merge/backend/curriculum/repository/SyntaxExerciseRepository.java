package com.merge.backend.curriculum.repository;

import com.merge.backend.curriculum.domain.SyntaxExercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SyntaxExerciseRepository extends JpaRepository<SyntaxExercise, Long> {

    Optional<SyntaxExercise> findByConceptId(Long conceptId);
}
