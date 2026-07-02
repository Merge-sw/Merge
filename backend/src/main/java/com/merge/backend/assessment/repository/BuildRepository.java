package com.merge.backend.assessment.repository;

import com.merge.backend.assessment.domain.Build;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BuildRepository extends JpaRepository<Build, Long> {

    Optional<Build> findByStudentIdAndStageName(Long studentId, String stageName);
}
