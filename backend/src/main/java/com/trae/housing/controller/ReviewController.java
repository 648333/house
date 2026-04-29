package com.trae.housing.controller;

import com.trae.housing.model.Property;
import com.trae.housing.model.Review;
import com.trae.housing.model.User;
import com.trae.housing.repository.PropertyRepository;
import com.trae.housing.repository.ReviewRepository;
import com.trae.housing.repository.UserRepository;
import com.trae.housing.service.InteractionTrackingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reviews")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class ReviewController {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private InteractionTrackingService interactionTrackingService;

    @GetMapping("/property/{propertyId}")
    public List<Review> getPropertyReviews(@PathVariable Long propertyId) {
        return reviewRepository.findByPropertyId(propertyId);
    }

    @GetMapping
    public ResponseEntity<?> getAllReviews() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != User.Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only admin can manage reviews");
        }

        return ResponseEntity.ok(reviewRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<?> createReview(@RequestBody Review reviewRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Property property = propertyRepository.findById(reviewRequest.getProperty().getId())
                .orElseThrow(() -> new RuntimeException("Property not found"));

        reviewRequest.setUser(user);
        reviewRequest.setProperty(property);

        Review saved = reviewRepository.save(reviewRequest);
        interactionTrackingService.track(user, property, InteractionTrackingService.ActionType.REVIEW, "review-create", null, 3.5);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteReview(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != User.Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only admin can delete reviews");
        }

        return reviewRepository.findById(id)
                .map(review -> {
                    reviewRepository.delete(review);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
