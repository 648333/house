package com.trae.housing.controller;

import com.trae.housing.model.Message;
import com.trae.housing.model.PriceAlertSubscription;
import com.trae.housing.model.Property;
import com.trae.housing.model.User;
import com.trae.housing.repository.MessageRepository;
import com.trae.housing.repository.PriceAlertSubscriptionRepository;
import com.trae.housing.repository.PropertyRepository;
import com.trae.housing.repository.UserRepository;
import com.trae.housing.service.InteractionTrackingService;
import com.trae.housing.service.PropertyRecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/properties")
@CrossOrigin(origins = "http://localhost:5173")
public class PropertyController {

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PropertyRecommendationService propertyRecommendationService;

    @Autowired
    private PriceAlertSubscriptionRepository priceAlertRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private InteractionTrackingService interactionTrackingService;

    @GetMapping
    public List<Property> getAllProperties() {
        return propertyRepository.findAll();
    }

    @GetMapping("/my")
    public List<Property> getMyProperties() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        return propertyRepository.findByOwnerUsername(currentUsername);
    }

    @GetMapping("/recommendations")
    public ResponseEntity<?> getRecommendedProperties(
            @RequestParam(defaultValue = "6") int limit,
            @RequestParam(defaultValue = "false") boolean explain) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (explain) {
            return ResponseEntity.ok(propertyRecommendationService.getRecommendationResultsForUser(currentUser, limit));
        }

        return ResponseEntity.ok(propertyRecommendationService.getRecommendationsForUser(currentUser, limit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Property> getPropertyById(@PathVariable Long id) {
        return propertyRepository.findById(id)
                .map(property -> {
                    trackInteractionIfAuthenticated(property, InteractionTrackingService.ActionType.VIEW, "property-detail", null, 1.0);
                    return ResponseEntity.ok(property);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createProperty(@RequestBody Property property) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        User owner = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        property.setOwner(owner);

        if (property.getContactName() == null || property.getContactName().trim().isEmpty()) {
            property.setContactName(owner.getUsername());
        }

        Property savedProperty = propertyRepository.save(property);
        return ResponseEntity.ok(savedProperty);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProperty(@PathVariable Long id, @RequestBody Property propertyRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return propertyRepository.findById(id)
                .map(property -> {
                    if (property.getOwner() == null || !property.getOwner().getId().equals(currentUser.getId())) {
                        return ResponseEntity.status(403).body("Not authorized to update this property");
                    }

                    BigDecimal oldPrice = property.getPrice();
                    property.setTitle(propertyRequest.getTitle());
                    property.setDescription(propertyRequest.getDescription());
                    property.setPrice(propertyRequest.getPrice());
                    property.setAddress(propertyRequest.getAddress());
                    property.setLatitude(propertyRequest.getLatitude());
                    property.setLongitude(propertyRequest.getLongitude());
                    property.setArea(propertyRequest.getArea());
                    property.setType(propertyRequest.getType());
                    property.setDecoration(propertyRequest.getDecoration());
                    property.setOrientation(propertyRequest.getOrientation());
                    property.setFloor(propertyRequest.getFloor());
                    property.setYearBuilt(propertyRequest.getYearBuilt());
                    property.setTags(propertyRequest.getTags());
                    property.setLayout(propertyRequest.getLayout());
                    property.setContactName(propertyRequest.getContactName());
                    property.setImageUrl(propertyRequest.getImageUrl());
                    property.setPanoramaUrl(propertyRequest.getPanoramaUrl());
                    property.setModel3dUrl(propertyRequest.getModel3dUrl());
                    property.setFloorPlanUrl(propertyRequest.getFloorPlanUrl());
                    property.setPanoramaImages(propertyRequest.getPanoramaImages());
                    property.setTourMetadata(propertyRequest.getTourMetadata());
                    property.setFurnishingPlan(propertyRequest.getFurnishingPlan());

                    Property updated = propertyRepository.save(property);
                    notifyPriceChange(updated, oldPrice, updated.getPrice());
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public List<Property> searchProperties(@RequestParam String title) {
        return propertyRepository.findByTitleContainingIgnoreCase(title);
    }

    @GetMapping("/commute-filter")
    public List<Property> commuteFilter(
            @RequestParam double centerLat,
            @RequestParam double centerLng,
            @RequestParam(defaultValue = "30") int minutes,
            @RequestParam(defaultValue = "25") double avgSpeedKmh
    ) {
        double maxDistanceKm = Math.max(1.0, minutes / 60.0 * avgSpeedKmh);
        return propertyRepository.findAll().stream()
                .filter(p -> p.getLatitude() != null && p.getLongitude() != null)
                .filter(p -> haversineKm(centerLat, centerLng, p.getLatitude(), p.getLongitude()) <= maxDistanceKm)
                .toList();
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updatePropertyStatus(@PathVariable Long id, @RequestParam Property.Status status) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return propertyRepository.findById(id)
                .map(property -> {
                    boolean isAdmin = currentUser.getRole() == User.Role.ADMIN;
                    boolean isOwner = property.getOwner() != null && property.getOwner().getId().equals(currentUser.getId());

                    if ((status == Property.Status.APPROVED || status == Property.Status.REJECTED) && !isAdmin) {
                        return ResponseEntity.status(403).body("Only admin can review property status");
                    }

                    if (status == Property.Status.SOLD && !(isAdmin || isOwner)) {
                        return ResponseEntity.status(403).body("Only owner or admin can mark property as sold");
                    }

                    property.setStatus(status);
                    return ResponseEntity.ok(propertyRepository.save(property));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private void notifyPriceChange(Property property, BigDecimal oldPrice, BigDecimal newPrice) {
        if (oldPrice == null || newPrice == null || oldPrice.compareTo(newPrice) == 0) {
            return;
        }

        List<PriceAlertSubscription> subscriptions = priceAlertRepository.findByPropertyIdAndActiveTrue(property.getId());
        if (subscriptions.isEmpty()) {
            return;
        }

        User sender = property.getOwner();
        for (PriceAlertSubscription sub : subscriptions) {
            if (sub.getTargetPrice() != null && newPrice.compareTo(sub.getTargetPrice()) > 0) {
                continue;
            }

            Message message = new Message();
            message.setSender(sender != null ? sender : sub.getUser());
            message.setReceiver(sub.getUser());
            message.setProperty(property);
            message.setContent("你订阅的房源价格已更新: " + property.getTitle() + "，由 " + oldPrice + " 调整为 " + newPrice + "。");
            messageRepository.save(message);
        }
    }

    private void trackInteractionIfAuthenticated(Property property,
                                                 InteractionTrackingService.ActionType actionType,
                                                 String source,
                                                 String metadata,
                                                 double weight) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            return;
        }

        userRepository.findByUsername(authentication.getName())
                .ifPresent(user -> interactionTrackingService.track(user, property, actionType, source, metadata, weight));
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
