package com.merge.backend.feedback.controller;

import com.merge.backend.assessment.repository.DrillSubmissionRepository;
import com.merge.backend.feedback.domain.CleanCodeFeedback;
import com.merge.backend.feedback.repository.CleanCodeFeedbackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class CleanCodeFeedbackControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DrillSubmissionRepository drillSubmissionRepository;

    @Mock
    private CleanCodeFeedbackRepository cleanCodeFeedbackRepository;

    @InjectMocks
    private CleanCodeFeedbackController cleanCodeFeedbackController;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(cleanCodeFeedbackController).build();
    }

    @Test
    public void testGetFeedback_SubmissionNotFound() throws Exception {
        when(drillSubmissionRepository.existsById(99L)).thenReturn(false);

        mockMvc.perform(get("/api/v1/submissions/99/feedback"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetFeedback_Generating() throws Exception {
        when(drillSubmissionRepository.existsById(10L)).thenReturn(true);
        when(cleanCodeFeedbackRepository.findByDrillSubmissionId(10L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/submissions/10/feedback"))
                .andExpect(status().isAccepted())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.generating").value(true))
                .andExpect(jsonPath("$.overallScore").doesNotExist());
    }

    @Test
    public void testGetFeedback_Ready() throws Exception {
        when(drillSubmissionRepository.existsById(10L)).thenReturn(true);

        CleanCodeFeedback feedback = new CleanCodeFeedback();
        feedback.setOverallScore(85);
        feedback.setNamingIssues(List.of("L5: Bad variable name"));
        feedback.setFunctionSizeIssues(List.of());
        feedback.setRedundancyIssues(List.of());
        feedback.setSolidIssues(List.of());
        feedback.setGeneratedAt(Instant.now());

        when(cleanCodeFeedbackRepository.findByDrillSubmissionId(10L)).thenReturn(Optional.of(feedback));

        mockMvc.perform(get("/api/v1/submissions/10/feedback"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.generating").value(false))
                .andExpect(jsonPath("$.overallScore").value(85))
                .andExpect(jsonPath("$.namingIssues[0]").value("L5: Bad variable name"));
    }
}
