package com.merge.backend.assessment.service;

import com.merge.backend.ai.gateway.GeminiGateway;
import com.merge.backend.assessment.domain.BuildComprehensionCheck;
import com.merge.backend.assessment.domain.BuildComprehensionCheckStatus;
import com.merge.backend.assessment.domain.BuildSubmission;
import com.merge.backend.assessment.domain.BuildSubmissionStatus;
import com.merge.backend.assessment.dto.BuildComprehensionScoreRequest;
import com.merge.backend.assessment.dto.BuildComprehensionSubmitRequest;
import com.merge.backend.assessment.dto.BuildComprehensionSubmitResponse;
import com.merge.backend.assessment.exception.BuildComprehensionTimerExpiredException;
import com.merge.backend.assessment.repository.BuildComprehensionCheckRepository;
import com.merge.backend.assessment.repository.BuildSubmissionRepository;
import com.merge.backend.identity.domain.Student;
import com.merge.backend.identity.repository.StudentRepository;
import com.merge.backend.progression.domain.ActivityType;
import com.merge.backend.progression.service.ProgressionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BuildComprehensionSubmitService {

    private final BuildComprehensionCheckRepository checkRepository;
    private final BuildSubmissionRepository buildSubmissionRepository;
    private final StudentRepository studentRepository;
    private final GeminiGateway geminiGateway;
    private final ProgressionService progressionService;

    @Transactional(noRollbackFor = BuildComprehensionTimerExpiredException.class)
    public BuildComprehensionSubmitResponse submit(Long checkId,
                                                   BuildComprehensionSubmitRequest request,
                                                   UserDetails userDetails) {
        BuildComprehensionCheck check = checkRepository.findById(checkId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Build comprehension check not found"));

        Student student = studentRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        if (!check.getStudent().getId().equals(student.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This comprehension check belongs to another student");
        }

        if (check.getStatus() != BuildComprehensionCheckStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Build comprehension check is already " + check.getStatus().name().toLowerCase());
        }

        if (Instant.now().isAfter(check.getServerDeadline())) {
            check.setStatus(BuildComprehensionCheckStatus.EXPIRED);
            checkRepository.save(check);
            BuildSubmission submission = check.getBuildSubmission();
            submission.setOverallStatus(BuildSubmissionStatus.FAILED);
            buildSubmissionRepository.save(submission);
            throw new BuildComprehensionTimerExpiredException();
        }

        List<String> answers = request.answers();
        List<String> validationErrors = new ArrayList<>();
        if (answers == null || answers.size() != check.getQuestions().size()) {
            validationErrors.add("Must provide exactly " + check.getQuestions().size() + " answers");
        } else {
            for (int i = 0; i < answers.size(); i++) {
                if (answers.get(i) == null || answers.get(i).isBlank()) {
                    validationErrors.add("Answer " + (i + 1) + " must not be blank");
                }
            }
        }
        if (!validationErrors.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.join("; ", validationErrors));
        }

        // Persist answers before the AI call so the attempt is recorded even if scoring throws.
        check.setAnswers(answers);
        check.setSubmittedAt(Instant.now());
        checkRepository.save(check);

        BuildSubmission submission = check.getBuildSubmission();
        boolean passed = geminiGateway.scoreBuildComprehensionAnswers(
                new BuildComprehensionScoreRequest(
                        submission.getCode(),
                        submission.getArchitectureDocument(),
                        submission.getTestSuite(),
                        check.getQuestions(),
                        answers
                ));

        if (!passed) {
            check.setStatus(BuildComprehensionCheckStatus.FAILED);
            checkRepository.save(check);
            submission.setOverallStatus(BuildSubmissionStatus.FAILED);
            buildSubmissionRepository.save(submission);
            return BuildComprehensionSubmitResponse.failed();
        }

        check.setStatus(BuildComprehensionCheckStatus.PASSED);
        checkRepository.save(check);

        submission.setGate4Passed(true);
        submission.setOverallStatus(BuildSubmissionStatus.PASSED);
        int awarded = progressionService.awardXp(
                student,
                submission.getPendingXp(),
                ActivityType.BUILD_PASS,
                submission.getBuild().getStageName(),
                submission.getBuild().getId()
        );
        submission.setXpAwarded(awarded);
        buildSubmissionRepository.save(submission);

        return BuildComprehensionSubmitResponse.passed(awarded);
    }
}
