package com.merge.backend.identity.service;

import com.merge.backend.identity.domain.Student;
import com.merge.backend.identity.dto.*;
import com.merge.backend.identity.repository.StudentRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;

    public StudentServiceImpl(StudentRepository studentRepository,
                              PasswordEncoder passwordEncoder,
                              JwtService jwtService,
                              TokenBlacklistService tokenBlacklistService) {
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    // ── ID-02: Registration ─────────────────────────────────────────────────────

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (studentRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("Email already registered: " + request.getEmail());
        }
        if (studentRepository.existsByUniversityEmail(request.getUniversityEmail())) {
            throw new DuplicateEmailException("University email already registered: " + request.getUniversityEmail());
        }

        Student student = new Student();
        student.setName(request.getName());
        student.setEmail(request.getEmail());
        student.setPhone(request.getPhone());
        student.setUniversityEmail(request.getUniversityEmail());
        student.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        student.setCurrentStage("SCOUT");
        student.setTotalXp(0);

        student = studentRepository.save(student);

        String accessToken = jwtService.generateAccessToken(student.getEmail());
        String refreshToken = jwtService.generateRefreshToken(student.getEmail());

        return new AuthResponse(accessToken, refreshToken, StudentResponse.from(student));
    }

    // ── ID-03: Login ────────────────────────────────────────────────────────────

    @Override
    public AuthResponse login(LoginRequest request) {
        Student student = studentRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), student.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String accessToken = jwtService.generateAccessToken(student.getEmail());
        String refreshToken = jwtService.generateRefreshToken(student.getEmail());

        return new AuthResponse(accessToken, refreshToken, StudentResponse.from(student));
    }

    // ── ID-03: Refresh ──────────────────────────────────────────────────────────

    @Override
    public AuthResponse refresh(String refreshToken) {
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        String email = jwtService.extractSubject(refreshToken);

        if (!jwtService.isTokenValid(refreshToken, email)) {
            throw new BadCredentialsException("Refresh token is expired or invalid");
        }

        if (tokenBlacklistService.isBlacklisted(refreshToken)) {
            throw new BadCredentialsException("Refresh token has been revoked");
        }

        Student student = studentRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Student not found"));

        String newAccessToken = jwtService.generateAccessToken(email);
        String newRefreshToken = jwtService.generateRefreshToken(email);

        // Rotate: blacklist old refresh token
        tokenBlacklistService.blacklist(refreshToken, 7L * 24 * 60 * 60);

        return new AuthResponse(newAccessToken, newRefreshToken, StudentResponse.from(student));
    }

    // ── ID-03: Logout ───────────────────────────────────────────────────────────

    @Override
    public void logout(String accessToken) {
        // Blacklist for 24 hours (access token TTL)
        tokenBlacklistService.blacklist(accessToken, 86400L);
    }

    // ── ID-04: Get profile ──────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public StudentResponse getProfile(String email) {
        Student student = studentRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + email));
        return StudentResponse.from(student);
    }

    // ── ID-04: Update profile ───────────────────────────────────────────────────

    @Override
    public StudentResponse updateProfile(String email, UpdateProfileRequest request) {
        Student student = studentRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + email));

        student.setName(request.getName());
        student.setPhone(request.getPhone());

        return StudentResponse.from(studentRepository.save(student));
    }
}
