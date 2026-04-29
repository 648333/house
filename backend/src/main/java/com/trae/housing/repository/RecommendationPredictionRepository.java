package com.trae.housing.repository;

import com.trae.housing.model.RecommendationPrediction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecommendationPredictionRepository extends JpaRepository<RecommendationPrediction, Long> {
    List<RecommendationPrediction> findByUserIdAndModelVersionOrderByScoreDesc(Long userId, String modelVersion);
    void deleteByModelVersion(String modelVersion);
}
