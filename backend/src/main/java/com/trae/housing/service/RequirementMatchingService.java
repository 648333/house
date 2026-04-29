package com.trae.housing.service;

import com.trae.housing.model.HouseRequirement;
import com.trae.housing.model.Property;
import com.trae.housing.repository.PropertyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class RequirementMatchingService {

    @Autowired
    private PropertyRepository propertyRepository;

    public List<Property> findMatches(HouseRequirement requirement, int limit) {
        return propertyRepository.findByStatus(Property.Status.APPROVED).stream()
                .sorted(Comparator.comparingDouble((Property property) -> score(requirement, property)).reversed())
                .limit(Math.max(limit, 1))
                .collect(Collectors.toList());
    }

    public long countStrongMatches(HouseRequirement requirement) {
        return propertyRepository.findByStatus(Property.Status.APPROVED).stream()
                .filter(property -> score(requirement, property) >= 7)
                .count();
    }

    private double score(HouseRequirement requirement, Property property) {
        double score = 0;

        if (containsIgnoreCase(property.getAddress(), requirement.getPreferredArea())) {
            score += 4;
        }

        if (containsIgnoreCase(property.getType(), requirement.getPropertyType())) {
            score += 2.5;
        }

        if (containsIgnoreCase(property.getLayout(), requirement.getLayoutPreference())) {
            score += 2.5;
        }

        if (containsIgnoreCase(property.getTags(), requirement.getLifestyleTags())) {
            score += 3;
        }

        score += matchAreaScore(requirement, property);
        score += matchBudgetScore(requirement, property.getPrice());
        score += containsIgnoreCase(property.getDescription(), requirement.getCommutePreference()) ? 1.5 : 0;

        return score;
    }

    private double matchAreaScore(HouseRequirement requirement, Property property) {
        if (property.getArea() == null) {
            return 0;
        }

        double score = 0;
        if (requirement.getMinArea() != null && property.getArea() >= requirement.getMinArea()) {
            score += 1.5;
        }
        if (requirement.getMaxArea() != null && property.getArea() <= requirement.getMaxArea()) {
            score += 1.5;
        }
        return score;
    }

    private double matchBudgetScore(HouseRequirement requirement, BigDecimal price) {
        if (price == null) {
            return 0;
        }

        double score = 0;
        if (requirement.getMinBudget() != null && price.compareTo(requirement.getMinBudget()) >= 0) {
            score += 1.5;
        }
        if (requirement.getMaxBudget() != null && price.compareTo(requirement.getMaxBudget()) <= 0) {
            score += 2;
        }
        return score;
    }

    private boolean containsIgnoreCase(String source, String query) {
        if (source == null || query == null || query.isBlank()) {
            return false;
        }

        String normalizedQuery = query.toLowerCase(Locale.ROOT);
        for (String part : source.split(",")) {
            if (part.toLowerCase(Locale.ROOT).contains(normalizedQuery)) {
                return true;
            }
        }

        return source.toLowerCase(Locale.ROOT).contains(normalizedQuery);
    }
}
