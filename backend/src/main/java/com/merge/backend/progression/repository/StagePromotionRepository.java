package com.merge.backend.progression.repository;

import com.merge.backend.progression.domain.StagePromotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StagePromotionRepository extends JpaRepository<StagePromotion, Long> {

    /** Returns true if the student has already been promoted out of the given stage. */
    boolean existsByStudentIdAndFromStage(Long studentId, String fromStage);
}
