package com.merge.backend.assessment.repository;

import com.merge.backend.assessment.domain.BuildGateResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BuildGateResultRepository extends JpaRepository<BuildGateResult, Long> {

    List<BuildGateResult> findByBuildSubmissionIdOrderByGateAsc(Long buildSubmissionId);
}
