package com.trae.housing.service;

import com.trae.housing.model.Appointment;
import com.trae.housing.model.HouseRequirement;
import com.trae.housing.model.Property;
import com.trae.housing.model.PropertyInteraction;
import com.trae.housing.model.RecommendationPrediction;
import com.trae.housing.model.Review;
import com.trae.housing.model.User;
import com.trae.housing.repository.AppointmentRepository;
import com.trae.housing.repository.HouseRequirementRepository;
import com.trae.housing.repository.PropertyInteractionRepository;
import com.trae.housing.repository.PropertyRepository;
import com.trae.housing.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PropertyRecommendationService {

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private HouseRequirementRepository requirementRepository;

    @Autowired
    private PropertyInteractionRepository propertyInteractionRepository;

    @Autowired
    private ModelRecommendationService modelRecommendationService;

    public List<Property> getRecommendationsForUser(User user, int limit) {
        return getRecommendationResultsForUser(user, limit).stream()
                .map(RecommendationResult::getProperty)
                .collect(Collectors.toList());
    }

    public List<RecommendationResult> getRecommendationResultsForUser(User user, int limit) {
        List<Property> approvedProperties = propertyRepository.findByStatus(Property.Status.APPROVED);
        if (approvedProperties.isEmpty()) {
            return List.of();
        }

        PreferenceProfile profile = buildPreferenceProfile(user);
        Map<Long, RecommendationPrediction> modelPredictions = modelRecommendationService.getPredictionsForUser(user.getId());

        return approvedProperties.stream()
                .map(property -> scorePropertyForUser(property, profile, modelPredictions.get(property.getId())))
                .sorted(Comparator.comparingDouble(RecommendationResult::getScore).reversed())
                .limit(Math.max(limit, 1))
                .collect(Collectors.toList());
    }

    private PreferenceProfile buildPreferenceProfile(User user) {
        PreferenceProfile profile = new PreferenceProfile();

        for (Appointment appointment : appointmentRepository.findByUserId(user.getId())) {
            addPropertySignals(profile, appointment.getProperty(), 4);
        }

        for (Review review : reviewRepository.findByUserId(user.getId())) {
            int weight = 2 + Math.max(review.getRating() == null ? 0 : review.getRating(), 0);
            addPropertySignals(profile, review.getProperty(), weight);
        }

        for (HouseRequirement requirement : requirementRepository.findByUserIdOrderByCreatedAtDesc(user.getId())) {
            if (requirement.getStatus() == HouseRequirement.Status.CLOSED) {
                continue;
            }
            addTextSignal(profile.areaPreference, requirement.getPreferredArea(), 5);
            addTextSignal(profile.typePreference, requirement.getPropertyType(), 4);
            addTextSignal(profile.layoutPreference, requirement.getLayoutPreference(), 3);
            addTags(profile.tagPreference, requirement.getLifestyleTags(), 3);
            mergeBudget(profile, requirement);
        }

        for (PropertyInteraction interaction : propertyInteractionRepository.findTop200ByUserIdOrderByCreatedAtDesc(user.getId())) {
            int weight = interactionWeight(interaction);
            addPropertySignals(profile, interaction.getProperty(), weight);
        }

        return profile;
    }

    private void addPropertySignals(PreferenceProfile profile, Property property, int weight) {
        if (property == null) {
            return;
        }
        addTags(profile.tagPreference, property.getTags(), weight);
        addTextSignal(profile.areaPreference, property.getAddress(), weight);
        addTextSignal(profile.typePreference, property.getType(), weight);
        addTextSignal(profile.layoutPreference, property.getLayout(), Math.max(1, weight - 1));
        if (property.getPrice() != null) {
            profile.targetPrice = property.getPrice();
        }
        if (property.getArea() != null) {
            profile.targetArea = property.getArea();
        }
    }

    private void addTags(Map<String, Integer> preference, String tags, int weight) {
        if (tags == null) {
            return;
        }

        for (String rawTag : tags.split(",")) {
            String normalizedTag = normalize(rawTag);
            if (!normalizedTag.isEmpty()) {
                preference.merge(normalizedTag, weight, Integer::sum);
            }
        }
    }

    private void addTextSignal(Map<String, Integer> preference, String value, int weight) {
        String normalized = normalize(value);
        if (!normalized.isEmpty()) {
            preference.merge(normalized, weight, Integer::sum);
        }
    }

    private void mergeBudget(PreferenceProfile profile, HouseRequirement requirement) {
        if (requirement.getMinBudget() != null) {
            profile.minBudget = requirement.getMinBudget();
        }
        if (requirement.getMaxBudget() != null) {
            profile.maxBudget = requirement.getMaxBudget();
        }
        if (requirement.getMinArea() != null) {
            profile.minArea = requirement.getMinArea();
        }
        if (requirement.getMaxArea() != null) {
            profile.maxArea = requirement.getMaxArea();
        }
    }

    private int interactionWeight(PropertyInteraction interaction) {
        if (interaction == null || interaction.getActionType() == null) {
            return 1;
        }

        return switch (interaction.getActionType()) {
            case FAVORITE -> 5;
            case APPOINTMENT -> 6;
            case REVIEW -> 4;
            case INQUIRY -> 4;
            case COMPARE -> 3;
            case TOUR_OPEN, TOUR_SCENE_VIEW -> 2;
            case SHARE -> 3;
            case VIEW -> 1;
        };
    }

    private RecommendationResult scorePropertyForUser(Property property,
                                                      PreferenceProfile profile,
                                                      RecommendationPrediction prediction) {
        double score = 0;
        List<String> reasons = new ArrayList<>();

        if (property.getTags() != null) {
            for (String rawTag : property.getTags().split(",")) {
                String tag = normalize(rawTag);
                int tagScore = profile.tagPreference.getOrDefault(tag, 0);
                if (tagScore > 0) {
                    score += tagScore;
                    reasons.add("matched tag: " + tag);
                }
            }
        }

        score += textPreferenceScore(profile.areaPreference, property.getAddress(), "area", reasons);
        score += textPreferenceScore(profile.typePreference, property.getType(), "type", reasons);
        score += textPreferenceScore(profile.layoutPreference, property.getLayout(), "layout", reasons);

        double budgetScore = budgetScore(profile, property.getPrice());
        if (budgetScore > 0) {
            score += budgetScore;
            reasons.add("budget friendly");
        }

        double areaScore = areaScore(profile, property.getArea());
        if (areaScore > 0) {
            score += areaScore;
            reasons.add("area fit");
        }

        if (property.getArea() != null && property.getArea() >= 90 && profile.minArea == null && profile.maxArea == null) {
            score += 1.5;
            reasons.add("comfortable area");
        }

        if (property.getPrice() != null) {
            score += Math.max(0, 8 - property.getPrice().doubleValue() / 2500.0);
        }

        double recency = recencyBoost(property.getCreatedAt());
        if (recency > 0) {
            score += recency;
            reasons.add("new listing");
        }

        if (prediction != null && prediction.getScore() != null) {
            score += prediction.getScore() * 0.2;
            reasons.add("model boost");
            if (prediction.getReason() != null && !prediction.getReason().isBlank()) {
                reasons.add(prediction.getReason());
            }
        }

        if (reasons.isEmpty()) {
            reasons.add("balanced match");
        }

        return new RecommendationResult(property, Math.round(score * 100.0) / 100.0, reasons.stream().limit(4).toList());
    }

    private double textPreferenceScore(Map<String, Integer> preference, String value, String reason, List<String> reasons) {
        String normalizedValue = normalize(value);
        if (normalizedValue.isEmpty()) {
            return 0;
        }

        double score = 0;
        for (Map.Entry<String, Integer> entry : preference.entrySet()) {
            if (normalizedValue.contains(entry.getKey()) || entry.getKey().contains(normalizedValue)) {
                score += entry.getValue();
                reasons.add("matched " + reason + ": " + entry.getKey());
            }
        }
        return score;
    }

    private double budgetScore(PreferenceProfile profile, BigDecimal price) {
        if (price == null) {
            return 0;
        }
        boolean aboveMin = profile.minBudget == null || price.compareTo(profile.minBudget) >= 0;
        boolean belowMax = profile.maxBudget == null || price.compareTo(profile.maxBudget) <= 0;
        if (aboveMin && belowMax && (profile.minBudget != null || profile.maxBudget != null)) {
            return 6;
        }
        if (profile.targetPrice != null) {
            double diff = Math.abs(price.doubleValue() - profile.targetPrice.doubleValue());
            return Math.max(0, 4 - diff / 250.0);
        }
        return 0;
    }

    private double areaScore(PreferenceProfile profile, Double area) {
        if (area == null) {
            return 0;
        }
        boolean aboveMin = profile.minArea == null || area >= profile.minArea;
        boolean belowMax = profile.maxArea == null || area <= profile.maxArea;
        if (aboveMin && belowMax && (profile.minArea != null || profile.maxArea != null)) {
            return 4;
        }
        if (profile.targetArea != null) {
            double diff = Math.abs(area - profile.targetArea);
            return Math.max(0, 3 - diff / 20.0);
        }
        return 0;
    }

    private double recencyBoost(LocalDateTime createdAt) {
        if (createdAt == null) {
            return 0;
        }

        long days = Math.abs(ChronoUnit.DAYS.between(createdAt, LocalDateTime.now()));
        return Math.max(0, 6 - days * 0.35);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static class PreferenceProfile {
        private final Map<String, Integer> tagPreference = new HashMap<>();
        private final Map<String, Integer> areaPreference = new HashMap<>();
        private final Map<String, Integer> typePreference = new HashMap<>();
        private final Map<String, Integer> layoutPreference = new HashMap<>();
        private BigDecimal minBudget;
        private BigDecimal maxBudget;
        private BigDecimal targetPrice;
        private Double minArea;
        private Double maxArea;
        private Double targetArea;
    }

    public static class RecommendationResult {
        private final Property property;
        private final double score;
        private final List<String> reasons;

        public RecommendationResult(Property property, double score, List<String> reasons) {
            this.property = property;
            this.score = score;
            this.reasons = reasons;
        }

        public Property getProperty() { return property; }
        public double getScore() { return score; }
        public List<String> getReasons() { return reasons; }
    }
}
