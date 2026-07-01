package com.merge.backend.identity.dto;

import com.merge.backend.identity.domain.Student;
import lombok.Data;

/**
 * Safe outbound representation of a student.
 * Never exposes passwordHash, githubOauthTokenEncrypted, or geminiTokenEncrypted.
 */
@Data
public class StudentResponse {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private String universityEmail;
    private String currentStage;
    private Integer totalXp;
    private String githubPortfolioRepo;
    private boolean githubConnected;

    public static StudentResponse from(Student student) {
        StudentResponse dto = new StudentResponse();
        dto.setId(student.getId());
        dto.setName(student.getName());
        dto.setEmail(student.getEmail());
        dto.setPhone(student.getPhone());
        dto.setUniversityEmail(student.getUniversityEmail());
        dto.setCurrentStage(student.getCurrentStage());
        dto.setTotalXp(student.getTotalXp());
        dto.setGithubPortfolioRepo(student.getGithubPortfolioRepo());
        dto.setGithubConnected(student.getGithubOauthTokenEncrypted() != null);
        return dto;
    }
}
