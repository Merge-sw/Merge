package com.merge.backend.curriculum.service;

import com.merge.backend.curriculum.domain.ConceptResource;
import com.merge.backend.curriculum.domain.ResourceCompletion;
import com.merge.backend.curriculum.repository.ConceptResourceRepository;
import com.merge.backend.curriculum.repository.ResourceCompletionRepository;
import com.merge.backend.identity.domain.Student;
import com.merge.backend.identity.repository.StudentRepository;
import com.merge.backend.progression.domain.ActivityType;
import com.merge.backend.progression.service.ProgressionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ResourceCompletionService {

    private static final int RESOURCE_XP = 5;

    private final ConceptResourceRepository resourceRepository;
    private final ResourceCompletionRepository completionRepository;
    private final StudentRepository studentRepository;
    private final ProgressionService progressionService;

    public ResourceCompletionService(ConceptResourceRepository resourceRepository,
                                     ResourceCompletionRepository completionRepository,
                                     StudentRepository studentRepository,
                                     ProgressionService progressionService) {
        this.resourceRepository = resourceRepository;
        this.completionRepository = completionRepository;
        this.studentRepository = studentRepository;
        this.progressionService = progressionService;
    }

    /**
     * Marks a resource as complete for the authenticated student.
     * XP is awarded only on first completion; the 50 XP per-stage cap on
     * LEARNING_RESOURCE activity type is enforced by ProgressionService.
     *
     * @return actual XP credited, or -1 as sentinel when already completed
     */
    @Transactional
    public int complete(Long resourceId, String studentEmail) {
        Student student = studentRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentEmail));

        ConceptResource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + resourceId));

        if (completionRepository.existsByStudentIdAndResourceId(student.getId(), resourceId)) {
            return -1;
        }

        ResourceCompletion completion = new ResourceCompletion();
        completion.setStudent(student);
        completion.setResource(resource);
        completion.setCompletedAt(Instant.now());
        completionRepository.save(completion);

        return progressionService.awardXp(
                student.getId(),
                RESOURCE_XP,
                ActivityType.LEARNING_RESOURCE,
                student.getCurrentStage(),
                resourceId).awarded();
    }
}
