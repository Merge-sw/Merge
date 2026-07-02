package com.merge.backend.assessment.service;

import com.merge.backend.ai.gateway.GeminiGateway;
import com.merge.backend.assessment.domain.BuildComprehensionCheck;
import com.merge.backend.assessment.domain.BuildComprehensionCheckStatus;
import com.merge.backend.assessment.domain.BuildSubmission;
import com.merge.backend.assessment.dto.BuildComprehensionQuestionsRequest;
import com.merge.backend.assessment.dto.ComprehensionQuestion;
import com.merge.backend.assessment.repository.BuildComprehensionCheckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BuildComprehensionTriggerService {

    private final GeminiGateway geminiGateway;
    private final BuildComprehensionCheckRepository buildComprehensionCheckRepository;

    static final int MIN_QUESTIONS = 4;
    static final int MAX_QUESTIONS = 6;

    /**
     * Generates Build comprehension questions from the student's specific artefacts
     * (code, architecture document, test strategies) and persists the check as PENDING.
     *
     * Called immediately when Judge0 returns status 3 (Gate 1 accepted).
     * Questions reference the student's actual variable names, function names,
     * data structures, and architectural choices — unanswerable without this exact code.
     *
     * serverDeadline = triggeredAt + (questionCount × 10 seconds)
     * Questions differ across submission attempts because they're generated from the
     * specific implementation, not a fixed bank.
     *
     * @return the saved BuildComprehensionCheck (caller uses its ID for the response)
     */
    public BuildComprehensionCheck triggerFor(BuildSubmission submission) {
        List<ComprehensionQuestion> generated = geminiGateway.generateBuildComprehensionQuestions(
                new BuildComprehensionQuestionsRequest(
                        submission.getCode(),
                        submission.getArchitectureDocument(),
                        submission.getTestSuite(),
                        submission.getBuild().getStageName(),
                        MIN_QUESTIONS,
                        MAX_QUESTIONS
                ));

        List<String> questionTexts = generated.stream()
                .map(ComprehensionQuestion::questionText)
                .limit(MAX_QUESTIONS)
                .toList();

        Instant now = Instant.now();
        Instant deadline = now.plusSeconds((long) questionTexts.size() * 10);

        BuildComprehensionCheck check = new BuildComprehensionCheck();
        check.setBuildSubmission(sub);
        check.setStudent(sub.getStudent());
        check.setQuestions(questionTexts);
        check.setTriggeredAt(now);
        check.setServerDeadline(deadline);
        check.setStatus(BuildComprehensionCheckStatus.PENDING);

        return buildComprehensionCheckRepository.save(check);
    }
}
