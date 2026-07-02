package com.merge.backend.curriculum.service;

import com.merge.backend.curriculum.domain.SyntaxExercise;
import com.merge.backend.curriculum.domain.SyntaxExerciseSubmission;
import com.merge.backend.curriculum.repository.ConceptRepository;
import com.merge.backend.curriculum.repository.SyntaxExerciseRepository;
import com.merge.backend.curriculum.repository.SyntaxExerciseSubmissionRepository;
import com.merge.backend.identity.domain.Student;
import com.merge.backend.identity.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class SyntaxExerciseService {

    private static final String CADET = "CADET";

    private final SyntaxExerciseRepository exerciseRepository;
    private final SyntaxExerciseSubmissionRepository submissionRepository;
    private final ConceptRepository conceptRepository;
    private final StudentRepository studentRepository;

    public SyntaxExerciseService(SyntaxExerciseRepository exerciseRepository,
                                 SyntaxExerciseSubmissionRepository submissionRepository,
                                 ConceptRepository conceptRepository,
                                 StudentRepository studentRepository) {
        this.exerciseRepository = exerciseRepository;
        this.submissionRepository = submissionRepository;
        this.conceptRepository = conceptRepository;
        this.studentRepository = studentRepository;
    }

    /**
     * Returns the syntax exercise for a concept.
     * Only available when the student's current stage is CADET — 404 for all other stages.
     * This keeps the resource invisible to Engineer+ students rather than returning a 403.
     */
    public SyntaxExercise getForConcept(Long conceptId, String studentEmail) {
        Student student = requireStudent(studentEmail);

        if (!CADET.equals(student.getCurrentStage())) {
            throw new ConceptNotFoundException("Syntax exercise not found for concept: " + conceptId);
        }

        if (!conceptRepository.existsById(conceptId)) {
            throw new ConceptNotFoundException("Concept not found: " + conceptId);
        }

        return exerciseRepository.findByConceptId(conceptId)
                .orElseThrow(() -> new ConceptNotFoundException(
                        "Syntax exercise not found for concept: " + conceptId));
    }

    /**
     * Records a submission and evaluates correctness by whitespace-normalised comparison
     * against the stored solution. No XP is awarded — syntax exercises are not assessed.
     */
    @Transactional
    public boolean submit(Long exerciseId, String submittedCode, String studentEmail) {
        Student student = requireStudent(studentEmail);

        SyntaxExercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new ConceptNotFoundException(
                        "Syntax exercise not found: " + exerciseId));

        boolean correct = normalise(submittedCode).equals(normalise(exercise.getSolutionCode()));

        SyntaxExerciseSubmission submission = new SyntaxExerciseSubmission();
        submission.setStudent(student);
        submission.setExercise(exercise);
        submission.setSubmittedCode(submittedCode);
        submission.setCorrect(correct);
        submission.setSubmittedAt(Instant.now());
        submissionRepository.save(submission);

        return correct;
    }

    private Student requireStudent(String email) {
        return studentRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + email));
    }

    /** Collapses all whitespace runs to a single space for lenient comparison. */
    private String normalise(String code) {
        return code == null ? "" : code.trim().replaceAll("\\s+", " ");
    }
}
