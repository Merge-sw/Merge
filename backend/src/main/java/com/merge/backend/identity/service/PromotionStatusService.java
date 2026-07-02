package com.merge.backend.identity.service;

import com.merge.backend.assessment.repository.BuildSubmissionRepository;
import com.merge.backend.curriculum.domain.Stage;
import com.merge.backend.curriculum.repository.StageRepository;
import com.merge.backend.identity.domain.Student;
import com.merge.backend.identity.dto.PromotionStatusResponse;
import com.merge.backend.identity.repository.StudentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PromotionStatusService {

    private final StudentRepository studentRepository;
    private final StageRepository stageRepository;
    private final BuildSubmissionRepository buildSubmissionRepository;

    public PromotionStatusService(StudentRepository studentRepository,
                                  StageRepository stageRepository,
                                  BuildSubmissionRepository buildSubmissionRepository) {
        this.studentRepository = studentRepository;
        this.stageRepository = stageRepository;
        this.buildSubmissionRepository = buildSubmissionRepository;
    }

    public PromotionStatusResponse check(String studentEmail) {
        Student student = studentRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Student not found"));

        Stage stage = stageRepository.findById(student.getCurrentStage())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Stage data missing for " + student.getCurrentStage()));

        int currentXp = student.getTotalXp();
        int currentBuildScore = buildSubmissionRepository
                .sumBestPassScoreByStudentAndStage(student.getId(), student.getCurrentStage());

        int missingXp = Math.max(0, stage.getXpThreshold() - currentXp);
        int missingBuildScore = Math.max(0, stage.getBuildPassScoreThreshold() - currentBuildScore);

        return new PromotionStatusResponse(
                missingXp == 0 && missingBuildScore == 0,
                missingXp,
                missingBuildScore
        );
    }
}
