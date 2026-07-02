package com.merge.backend.curriculum.service;

import com.merge.backend.curriculum.domain.Stage;
import com.merge.backend.curriculum.repository.StageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class StageService {

    private final StageRepository stageRepository;

    public StageService(StageRepository stageRepository) {
        this.stageRepository = stageRepository;
    }

    public List<Stage> getAllStages() {
        return stageRepository.findAll();
    }

    public Stage getStage(String stageType) {
        return stageRepository.findById(stageType.toUpperCase())
                .orElseThrow(() -> new StageNotFoundException("Stage not found: " + stageType));
    }
}
