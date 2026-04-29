package com.trae.housing.controller;

import com.trae.housing.model.Property;
import com.trae.housing.model.PropertyInteraction;
import com.trae.housing.model.User;
import com.trae.housing.repository.PropertyRepository;
import com.trae.housing.repository.UserRepository;
import com.trae.housing.service.InteractionTrackingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/interactions")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class InteractionController {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private InteractionTrackingService interactionTrackingService;

    @PostMapping
    public ResponseEntity<?> track(@RequestBody Map<String, Object> payload) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Object propertyIdRaw = payload == null ? null : payload.get("propertyId");
        Object actionTypeRaw = payload == null ? null : payload.get("actionType");
        if (propertyIdRaw == null || actionTypeRaw == null) {
            return ResponseEntity.badRequest().body("propertyId and actionType are required");
        }

        Long propertyId = Long.valueOf(String.valueOf(propertyIdRaw));
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new RuntimeException("Property not found"));

        InteractionTrackingService.ActionType actionType = InteractionTrackingService.ActionType.valueOf(String.valueOf(actionTypeRaw));
        String source = payload.get("source") == null ? null : String.valueOf(payload.get("source"));
        String metadata = payload.get("metadata") == null ? null : String.valueOf(payload.get("metadata"));
        double weight = payload.get("weight") == null ? 1.0 : Double.parseDouble(String.valueOf(payload.get("weight")));

        interactionTrackingService.track(user, property, actionType, source, metadata, weight);
        return ResponseEntity.ok(Map.of("tracked", true));
    }

    @GetMapping("/my")
    public List<PropertyInteraction> myInteractions() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return interactionTrackingService.getRecentInteractionsForUser(user.getId());
    }
}
