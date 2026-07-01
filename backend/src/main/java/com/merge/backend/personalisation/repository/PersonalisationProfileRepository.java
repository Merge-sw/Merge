package com.merge.backend.personalisation.repository;

import com.merge.backend.personalisation.domain.PersonalisationProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PersonalisationProfileRepository extends JpaRepository<PersonalisationProfile, Long> {
    Optional<PersonalisationProfile> findByStudentId(Long studentId);
}
