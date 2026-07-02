package com.merge.backend.assessment.service;

import com.merge.backend.assessment.domain.CodeReadingSubmission;
import com.merge.backend.assessment.domain.Drill;
import com.merge.backend.assessment.repository.CodeReadingSubmissionRepository;
import com.merge.backend.assessment.repository.DrillRepository;
import com.merge.backend.curriculum.service.ConceptNotFoundException;
import com.merge.backend.identity.domain.Student;
import com.merge.backend.identity.repository.StudentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
public class CodeReadingService {

    private final DrillRepository drillRepository;
    private final CodeReadingSubmissionRepository submissionRepository;
    private final StudentRepository studentRepository;

    public CodeReadingService(DrillRepository drillRepository,
                              CodeReadingSubmissionRepository submissionRepository,
                              StudentRepository studentRepository) {
        this.drillRepository = drillRepository;
        this.submissionRepository = submissionRepository;
        this.studentRepository = studentRepository;
    }

    /**
     * Records the student's code reading response for Drill 2.
     * Code reading is only required for Drill 2 — calling this on Drill 1 returns 400.
     * Idempotent: re-submitting after first acceptance returns accepted=true without
     * creating a duplicate record (UNIQUE constraint on student+drill).
     *
     * @return true always on success; the controller maps this to { accepted: true }
     */
    @Transactional
    public boolean submit(Long drillId, String responseText, String studentEmail) {
        Student student = studentRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentEmail));

        Drill drill = drillRepository.findById(drillId)
                .orElseThrow(() -> new ConceptNotFoundException("Drill not found: " + drillId));

        if (drill.getDrillNumber() != 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Code reading is only required for Drill 2");
        }

        if (submissionRepository.existsByStudentIdAndDrillId(student.getId(), drillId)) {
            return true;
        }

        CodeReadingSubmission submission = new CodeReadingSubmission();
        submission.setStudent(student);
        submission.setDrill(drill);
        submission.setResponseText(responseText);
        submission.setSubmittedAt(Instant.now());
        submissionRepository.save(submission);

        return true;
    }
}
