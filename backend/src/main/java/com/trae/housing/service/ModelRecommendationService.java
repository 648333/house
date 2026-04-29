package com.trae.housing.service;

import com.trae.housing.model.RecommendationPrediction;
import com.trae.housing.repository.RecommendationPredictionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ModelRecommendationService {
    @Autowired
    private RecommendationPredictionRepository recommendationPredictionRepository;

    @Value("${trae.app.recommendation.active-model-version:}")
    private String activeModelVersion;

    public Map<Long, RecommendationPrediction> getPredictionsForUser(Long userId) {
        if (activeModelVersion == null || activeModelVersion.isBlank()) {
            return Map.of();
        }

        return recommendationPredictionRepository.findByUserIdAndModelVersionOrderByScoreDesc(userId, activeModelVersion)
                .stream()
                .collect(Collectors.toMap(
                        RecommendationPrediction::getPropertyId,
                        prediction -> prediction,
                        (left, right) -> left
                ));
    }

    public void replacePredictions(String modelVersion, List<RecommendationPrediction> predictions) {
        recommendationPredictionRepository.deleteByModelVersion(modelVersion);
        recommendationPredictionRepository.saveAll(predictions);
    }
}
