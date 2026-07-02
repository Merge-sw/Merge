package com.merge.backend.curriculum.repository;

import com.merge.backend.curriculum.domain.SyntaxExerciseSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SyntaxExerciseSubmissionRepository extends JpaRepository<SyntaxExerciseSubmission, Long> {
}
