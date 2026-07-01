package com.merge.backend.identity.service;

import com.merge.backend.identity.domain.Student;
import com.merge.backend.identity.dto.StudentResponse;
import com.merge.backend.identity.repository.StudentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Validates a student-supplied Gemini API key against the live Gemini API,
 * then AES-256-GCM encrypts it and persists the ciphertext only.
 * The plaintext token is never logged, stored, or returned to the caller.
 */
@Service
@Transactional
public class GeminiTokenService {

    private static final String GEMINI_MODELS_URL =
            "https://generativelanguage.googleapis.com/v1beta/models?key=";

    private final StudentRepository studentRepository;
    private final TokenEncryptionService tokenEncryptionService;
    private final RestTemplate restTemplate;

    public GeminiTokenService(StudentRepository studentRepository,
                              TokenEncryptionService tokenEncryptionService) {
        this.studentRepository = studentRepository;
        this.tokenEncryptionService = tokenEncryptionService;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Validates the supplied token with the Gemini models endpoint,
     * encrypts it, and stores the ciphertext on the student record.
     *
     * @return StudentResponse — never contains the plaintext token.
     */
    public StudentResponse saveToken(String studentEmail, String plainToken) {
        validateWithGemini(plainToken);

        String encrypted = tokenEncryptionService.encrypt(plainToken);

        Student student = studentRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentEmail));

        student.setGeminiTokenEncrypted(encrypted);
        return StudentResponse.from(studentRepository.save(student));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void validateWithGemini(String token) {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    GEMINI_MODELS_URL + token, Map.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new InvalidGeminiTokenException("Gemini API key is not valid");
            }
        } catch (HttpClientErrorException e) {
            throw new InvalidGeminiTokenException(
                    "Gemini API key rejected: " + e.getStatusCode());
        } catch (InvalidGeminiTokenException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidGeminiTokenException(
                    "Unable to reach Gemini API for key validation");
        }
    }
}
