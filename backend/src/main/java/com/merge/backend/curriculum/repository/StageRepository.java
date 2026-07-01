package com.merge.backend.curriculum.repository;

import com.merge.backend.curriculum.domain.Stage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StageRepository extends JpaRepository<Stage, String> {}
