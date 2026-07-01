package com.merge.backend.identity.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "students")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String phone;

    @Column(name = "university_email", nullable = false, unique = true)
    private String universityEmail;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "current_stage", nullable = false)
    private String currentStage;

    @Column(name = "total_xp", nullable = false)
    private Integer totalXp = 0;

    @Column(name = "github_oauth_token_encrypted")
    private String githubOauthTokenEncrypted;

    @Column(name = "github_portfolio_repo")
    private String githubPortfolioRepo;

    @Column(name = "gemini_token_encrypted")
    private String geminiTokenEncrypted;
}
