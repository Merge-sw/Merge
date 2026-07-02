package com.merge.backend.feedback.controller;

import com.merge.backend.assessment.repository.DrillSubmissionRepository;
import com.merge.backend.feedback.domain.CleanCodeFeedback;
import com.merge.backend.feedback.dto.CleanCodeFeedbackResponse;
import com.merge.backend.feedback.repository.CleanCodeFeedbackRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/submissions")
public class CleanCodeFeedbackController {

    private final DrillSubmissionRepository drillSubmissionRepository;
    private final CleanCodeFeedbackRepository cleanCodeFeedbackRepository;

    public CleanCodeFeedbackController(DrillSubmissionRepository drillSubmissionRepository,
                                       CleanCodeFeedbackRepository cleanCodeFeedbackRepository) {
        this.drillSubmissionRepository = drillSubmissionRepository;
        this.cleanCodeFeedbackRepository = cleanCodeFeedbackRepository;
    }

    @GetMapping("/{id}/feedback")
    public ResponseEntity<CleanCodeFeedbackResponse> getFeedback(@PathVariable("id") Long submissionId) {
        // 1. Verify drill submission exists
        boolean submissionExists = drillSubmissionRepository.existsById(submissionId);
        if (!submissionExists) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found with id: " + submissionId);
        }

        // 2. Fetch feedback
        var feedbackOpt = cleanCodeFeedbackRepository.findByDrillSubmissionId(submissionId);
        if (feedbackOpt.isEmpty()) {
            // Return 202 Accepted if still generating
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(CleanCodeFeedbackResponse.emptyGenerating());
        }

        CleanCodeFeedback f = feedbackOpt.get();
        CleanCodeFeedbackResponse response = new CleanCodeFeedbackResponse(
                false,
                f.getOverallScore(),
                f.getNamingIssues(),
                f.getFunctionSizeIssues(),
                f.getRedundancyIssues(),
                f.getSolidIssues(),
                f.getGeneratedAt()
        );

        return ResponseEntity.ok(response);
    }
}
