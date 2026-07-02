package com.merge.backend.progression.repository;

import com.merge.backend.progression.domain.ActivityType;
import com.merge.backend.progression.domain.XpCap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface XpCapRepository extends JpaRepository<XpCap, Long> {

    /**
     * Returns the cap row for the given stage + activity combination.
     * Empty means no cap is configured for this pair — award is uncapped.
     */
    Optional<XpCap> findByStageNameAndActivityType(String stageName, ActivityType activityType);
}
