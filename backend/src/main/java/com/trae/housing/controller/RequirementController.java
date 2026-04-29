package com.trae.housing.controller;

import com.trae.housing.model.HouseRequirement;
import com.trae.housing.model.Property;
import com.trae.housing.model.User;
import com.trae.housing.repository.HouseRequirementRepository;
import com.trae.housing.repository.UserRepository;
import com.trae.housing.service.RequirementMatchingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/requirements")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class RequirementController {
    private static final int MAX_ACTIVE_REQUIREMENTS = 5;

    @Autowired
    private HouseRequirementRepository requirementRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RequirementMatchingService requirementMatchingService;

    @GetMapping("/my")
    public List<Map<String, Object>> getMyRequirements() {
        User currentUser = getCurrentUser();
        normalizeInvalidRequirements(currentUser);
        return requirementRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId()).stream()
                .filter(this::isMeaningfulRequirement)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/agent")
    public ResponseEntity<?> getAgentRequirementBoard() {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != User.Role.AGENT && currentUser.getRole() != User.Role.ADMIN) {
            return ResponseEntity.status(403).body("Not authorized");
        }

        List<Map<String, Object>> response = requirementRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(this::isMeaningfulRequirement)
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<?> createRequirement(@RequestBody HouseRequirement request) {
        User currentUser = getCurrentUser();
        normalizeInvalidRequirements(currentUser);
        long activeCount = requirementRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId()).stream()
                .filter(this::isMeaningfulRequirement)
                .filter(item -> item.getStatus() != HouseRequirement.Status.CLOSED)
                .count();

        if (activeCount >= MAX_ACTIVE_REQUIREMENTS) {
            return ResponseEntity.badRequest().body("Requirement limit reached");
        }

        request.setId(null);
        request.setUser(currentUser);
        request.setAssignedAgent(null);
        request.setStatus(HouseRequirement.Status.OPEN);

        HouseRequirement saved = requirementRepository.save(request);
        long matchCount = requirementMatchingService.countStrongMatches(saved);
        if (matchCount > 0) {
            saved.setStatus(HouseRequirement.Status.MATCHED);
            saved = requirementRepository.save(saved);
        }

        return ResponseEntity.ok(toResponse(saved));
    }

    @PutMapping("/{id}/assign")
    public ResponseEntity<?> assignRequirement(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != User.Role.AGENT && currentUser.getRole() != User.Role.ADMIN) {
            return ResponseEntity.status(403).body("Not authorized");
        }

        HouseRequirement requirement = requirementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Requirement not found"));

        requirement.setAssignedAgent(currentUser);
        requirement.setStatus(HouseRequirement.Status.FOLLOWING);
        return ResponseEntity.ok(toResponse(requirementRepository.save(requirement)));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestParam HouseRequirement.Status status) {
        User currentUser = getCurrentUser();
        HouseRequirement requirement = requirementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Requirement not found"));

        boolean canUpdate = currentUser.getRole() == User.Role.ADMIN
                || (requirement.getAssignedAgent() != null && requirement.getAssignedAgent().getId().equals(currentUser.getId()))
                || requirement.getUser().getId().equals(currentUser.getId());

        if (!canUpdate) {
            return ResponseEntity.status(403).body("Not authorized");
        }

        requirement.setStatus(status);
        return ResponseEntity.ok(toResponse(requirementRepository.save(requirement)));
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private Map<String, Object> toResponse(HouseRequirement requirement) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", requirement.getId());
        payload.put("title", requirement.getTitle());
        payload.put("preferredArea", requirement.getPreferredArea());
        payload.put("propertyType", requirement.getPropertyType());
        payload.put("layoutPreference", requirement.getLayoutPreference());
        payload.put("minArea", requirement.getMinArea());
        payload.put("maxArea", requirement.getMaxArea());
        payload.put("minBudget", requirement.getMinBudget());
        payload.put("maxBudget", requirement.getMaxBudget());
        payload.put("commutePreference", requirement.getCommutePreference());
        payload.put("lifestyleTags", requirement.getLifestyleTags());
        payload.put("note", requirement.getNote());
        payload.put("status", requirement.getStatus());
        payload.put("createdAt", requirement.getCreatedAt());
        payload.put("user", requirement.getUser());
        payload.put("assignedAgent", requirement.getAssignedAgent());

        List<Property> matches = requirementMatchingService.findMatches(requirement, 3);
        payload.put("matchedProperties", matches);
        payload.put("matchCount", requirementMatchingService.countStrongMatches(requirement));
        return payload;
    }

    private void normalizeInvalidRequirements(User user) {
        List<HouseRequirement> requirements = requirementRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        boolean changed = false;
        for (HouseRequirement requirement : requirements) {
            if (!isMeaningfulRequirement(requirement) && requirement.getStatus() != HouseRequirement.Status.CLOSED) {
                requirement.setStatus(HouseRequirement.Status.CLOSED);
                changed = true;
            }
        }
        if (changed) {
            requirementRepository.saveAll(requirements);
        }
    }

    private boolean isMeaningfulRequirement(HouseRequirement requirement) {
        if (requirement == null) {
            return false;
        }

        return hasText(requirement.getTitle())
                || hasText(requirement.getPreferredArea())
                || hasText(requirement.getPropertyType())
                || hasText(requirement.getLayoutPreference())
                || hasText(requirement.getLifestyleTags())
                || hasText(requirement.getNote())
                || requirement.getMinBudget() != null
                || requirement.getMaxBudget() != null
                || requirement.getMinArea() != null
                || requirement.getMaxArea() != null;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
