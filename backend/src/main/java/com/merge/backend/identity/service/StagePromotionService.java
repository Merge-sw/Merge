package com.merge.backend.identity.service;

import com.merge.backend.assessment.repository.BuildSubmissionRepository;
import com.merge.backend.identity.domain.Student;
import com.merge.backend.identity.dto.PromoteResponse;
import com.merge.backend.identity.dto.PromotionStatusResponse;
import com.merge.backend.identity.repository.StudentRepository;
import com.merge.backend.progression.domain.StagePromotion;
import com.merge.backend.progression.repository.StagePromotionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class StagePromotionService {

    private static final List<String> STAGE_ORDER =
            List.of("SCOUT", "CADET", "ENGINEER", "ARCHITECT", "PRINCIPAL");

    private final StudentRepository studentRepository;
    private final StagePromotionRepository stagePromotionRepository;
    private final BuildSubmissionRepository buildSubmissionRepository;
    private final PromotionStatusService promotionStatusService;

    public StagePromotionService(StudentRepository studentRepository,
                                 StagePromotionRepository stagePromotionRepository,
                                 BuildSubmissionRepository buildSubmissionRepository,
                                 PromotionStatusService promotionStatusService) {
        this.studentRepository = studentRepository;
        this.stagePromotionRepository = stagePromotionRepository;
        this.buildSubmissionRepository = buildSubmissionRepository;
        this.promotionStatusService = promotionStatusService;
    }

    @Transactional
    public PromoteResponse promote(String studentEmail) {
        Student student = studentRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Student not found"));

        String currentStage = student.getCurrentStage();
        int idx = STAGE_ORDER.indexOf(currentStage);

        // 409: no next stage exists (PRINCIPAL, or unrecognised stage name)
        if (idx < 0 || idx >= STAGE_ORDER.size() - 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Already at the highest stage");
        }

        // 409: this stage-to-next transition was already recorded
        // The unique constraint on (student_id, from_stage) also enforces this at the DB level.
        if (stagePromotionRepository.existsByStudentIdAndFromStage(student.getId(), currentStage)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Already promoted from " + currentStage);
        }

        // 403: one or both gates not yet met — delegate to PR-04
        PromotionStatusResponse status = promotionStatusService.check(studentEmail);
        if (!status.eligible()) {
            throw new PromotionNotEligibleException(status.missingXp(), status.missingBuildScore());
        }

        String nextStage = STAGE_ORDER.get(idx + 1);
        int xpAtPromotion = student.getTotalXp();
        int buildScoreAtPromotion = buildSubmissionRepository
                .sumBestPassScoreByStudentAndStage(student.getId(), currentStage);

        student.setCurrentStage(nextStage);
        studentRepository.save(student);

        StagePromotion promotion = new StagePromotion();
        promotion.setStudent(student);
        promotion.setFromStage(currentStage);
        promotion.setToStage(nextStage);
        promotion.setXpAtPromotion(xpAtPromotion);
        promotion.setBuildScoreAtPromotion(buildScoreAtPromotion);
        promotion.setPromotedAt(Instant.now());
        stagePromotionRepository.save(promotion);

        return new PromoteResponse(currentStage, nextStage, xpAtPromotion, buildScoreAtPromotion);
    }
}
