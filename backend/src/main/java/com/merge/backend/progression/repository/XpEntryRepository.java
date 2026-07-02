package com.merge.backend.progression.repository;

import com.merge.backend.progression.domain.ActivityType;
import com.merge.backend.progression.domain.XpEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface XpEntryRepository extends JpaRepository<XpEntry, Long> {

    @Query("SELECT COALESCE(SUM(e.xpAmount), 0) FROM XpEntry e " +
           "WHERE e.student.id = :studentId " +
           "AND e.stageType = :stageType " +
           "AND e.activityType = :activityType")
    int sumByStudentIdAndStageTypeAndActivityType(
            @Param("studentId") Long studentId,
            @Param("stageType") String stageType,
            @Param("activityType") ActivityType activityType);
}
