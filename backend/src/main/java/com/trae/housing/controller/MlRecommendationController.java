package com.trae.housing.controller;

import com.trae.housing.model.*;
import com.trae.housing.repository.*;
import com.trae.housing.service.ModelRecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/ml/recommendations")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class MlRecommendationController {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private PropertyInteractionRepository propertyInteractionRepository;

    @Autowired
    private HouseRequirementRepository houseRequirementRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ModelRecommendationService modelRecommendationService;

    @GetMapping("/dataset")
    public ResponseEntity<?> exportDataset() {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != User.Role.ADMIN) {
            return ResponseEntity.status(403).body("Only admin can export ML datasets");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("users", userRepository.findAll().stream().map(user -> Map.of(
                "id", user.getId(),
                "role", String.valueOf(user.getRole()),
                "enabled", user.isEnabled()
        )).toList());
        payload.put("properties", propertyRepository.findAll().stream().map(property -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", property.getId());
            row.put("price", property.getPrice());
            row.put("area", property.getArea());
            row.put("type", property.getType());
            row.put("layout", property.getLayout());
            row.put("address", property.getAddress());
            row.put("tags", property.getTags());
            row.put("status", String.valueOf(property.getStatus()));
            row.put("createdAt", property.getCreatedAt());
            row.put("hasPanorama", property.getPanoramaUrl() != null || property.getPanoramaImages() != null);
            row.put("hasModel3d", property.getModel3dUrl() != null);
            row.put("hasFloorPlan", property.getFloorPlanUrl() != null);
            return row;
        }).toList());
        payload.put("interactions", propertyInteractionRepository.findAll().stream().map(interaction -> Map.of(
                "userId", interaction.getUser().getId(),
                "propertyId", interaction.getProperty().getId(),
                "actionType", String.valueOf(interaction.getActionType()),
                "weight", interaction.getWeight(),
                "source", String.valueOf(interaction.getSource()),
                "metadata", String.valueOf(interaction.getMetadata()),
                "createdAt", interaction.getCreatedAt()
        )).toList());
        payload.put("requirements", houseRequirementRepository.findAll().stream().map(req -> Map.of(
                "userId", req.getUser().getId(),
                "preferredArea", String.valueOf(req.getPreferredArea()),
                "propertyType", String.valueOf(req.getPropertyType()),
                "layoutPreference", String.valueOf(req.getLayoutPreference()),
                "minBudget", req.getMinBudget(),
                "maxBudget", req.getMaxBudget(),
                "minArea", req.getMinArea(),
                "maxArea", req.getMaxArea(),
                "lifestyleTags", String.valueOf(req.getLifestyleTags()),
                "status", String.valueOf(req.getStatus())
        )).toList());
        payload.put("appointments", appointmentRepository.findAll().stream().map(item -> Map.of(
                "userId", item.getUser().getId(),
                "propertyId", item.getProperty().getId(),
                "status", String.valueOf(item.getStatus()),
                "createdAt", item.getCreatedAt()
        )).toList());
        payload.put("reviews", reviewRepository.findAll().stream().map(item -> Map.of(
                "userId", item.getUser().getId(),
                "propertyId", item.getProperty().getId(),
                "rating", item.getRating(),
                "createdAt", item.getCreatedAt()
        )).toList());

        return ResponseEntity.ok(payload);
    }

    @PostMapping("/predictions/import")
    public ResponseEntity<?> importPredictions(@RequestBody PredictionImportRequest request) {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != User.Role.ADMIN) {
            return ResponseEntity.status(403).body("Only admin can import ML predictions");
        }
        if (request == null || request.getModelVersion() == null || request.getModelVersion().isBlank()) {
            return ResponseEntity.badRequest().body("modelVersion is required");
        }

        List<RecommendationPrediction> predictions = Optional.ofNullable(request.getPredictions())
                .orElse(List.of())
                .stream()
                .map(item -> {
                    RecommendationPrediction prediction = new RecommendationPrediction();
                    prediction.setUserId(item.getUserId());
                    prediction.setPropertyId(item.getPropertyId());
                    prediction.setScore(item.getScore());
                    prediction.setReason(item.getReason());
                    prediction.setModelName(request.getModelName() == null ? "offline-ranker" : request.getModelName());
                    prediction.setModelVersion(request.getModelVersion());
                    return prediction;
                })
                .collect(Collectors.toList());

        modelRecommendationService.replacePredictions(request.getModelVersion(), predictions);
        return ResponseEntity.ok(Map.of(
                "imported", predictions.size(),
                "modelVersion", request.getModelVersion()
        ));
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        return userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public static class PredictionImportRequest {
        private String modelVersion;
        private String modelName;
        private List<PredictionItem> predictions;

        public String getModelVersion() { return modelVersion; }
        public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
        public String getModelName() { return modelName; }
        public void setModelName(String modelName) { this.modelName = modelName; }
        public List<PredictionItem> getPredictions() { return predictions; }
        public void setPredictions(List<PredictionItem> predictions) { this.predictions = predictions; }
    }

    public static class PredictionItem {
        private Long userId;
        private Long propertyId;
        private Double score;
        private String reason;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public Long getPropertyId() { return propertyId; }
        public void setPropertyId(Long propertyId) { this.propertyId = propertyId; }
        public Double getScore() { return score; }
        public void setScore(Double score) { this.score = score; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}
