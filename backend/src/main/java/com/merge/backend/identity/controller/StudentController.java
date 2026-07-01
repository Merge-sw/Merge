package com.merge.backend.identity.controller;

import com.merge.backend.identity.dto.StudentResponse;
import com.merge.backend.identity.dto.UpdateProfileRequest;
import com.merge.backend.identity.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    /**
     * ID-04: GET /api/v1/students/me
     * Returns full student record for the authenticated student.
     * Requires valid JWT.
     */
    @GetMapping("/me")
    public ResponseEntity<StudentResponse> getOwnProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        StudentResponse profile = studentService.getProfile(userDetails.getUsername());
        return ResponseEntity.ok(profile);
    }

    /**
     * ID-04: PUT /api/v1/students/me
     * Updates name and phone for the authenticated student.
     * Requires valid JWT.
     */
    @PutMapping("/me")
    public ResponseEntity<StudentResponse> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {
        StudentResponse updated = studentService.updateProfile(userDetails.getUsername(), request);
        return ResponseEntity.ok(updated);
    }
}
