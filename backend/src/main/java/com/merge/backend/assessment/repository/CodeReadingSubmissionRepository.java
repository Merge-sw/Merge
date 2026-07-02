package com.merge.backend.assessment.repository;

import com.merge.backend.assessment.domain.CodeReadingSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CodeReadingSubmissionRepository extends JpaRepository<CodeReadingSubmission, Long> {

    boolean existsByStudentIdAndDrillId(Long studentId, Long drillId);
}
