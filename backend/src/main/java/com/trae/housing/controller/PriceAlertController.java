package com.trae.housing.controller;

import com.trae.housing.model.Message;
import com.trae.housing.model.PriceAlertSubscription;
import com.trae.housing.model.Property;
import com.trae.housing.model.User;
import com.trae.housing.repository.MessageRepository;
import com.trae.housing.repository.PriceAlertSubscriptionRepository;
import com.trae.housing.repository.PropertyRepository;
import com.trae.housing.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/price-alerts")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"}, allowCredentials = "true")
public class PriceAlertController {

    @Autowired
    private PriceAlertSubscriptionRepository priceAlertRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageRepository messageRepository;

    @GetMapping("/my")
    public List<PriceAlertSubscription> mySubscriptions() {
        return priceAlertRepository.findByUserIdOrderByCreatedAtDesc(currentUser().getId());
    }

    @PostMapping
    public ResponseEntity<?> subscribe(@RequestBody Map<String, Object> payload) {
        User current = currentUser();
        Long propertyId = payload.get("propertyId") == null ? null : Long.parseLong(String.valueOf(payload.get("propertyId")));
        if (propertyId == null) {
            return ResponseEntity.badRequest().body("propertyId is required");
        }

        Property property = propertyRepository.findById(propertyId).orElseThrow(() -> new RuntimeException("Property not found"));
        PriceAlertSubscription subscription = priceAlertRepository.findByUserIdAndPropertyId(current.getId(), propertyId)
                .orElseGet(PriceAlertSubscription::new);

        subscription.setUser(current);
        subscription.setProperty(property);
        subscription.setActive(true);
        if (payload.get("targetPrice") != null) {
            subscription.setTargetPrice(new BigDecimal(String.valueOf(payload.get("targetPrice"))));
        }

        return ResponseEntity.ok(priceAlertRepository.save(subscription));
    }

    @DeleteMapping("/property/{propertyId}")
    public ResponseEntity<?> unsubscribe(@PathVariable Long propertyId) {
        User current = currentUser();
        PriceAlertSubscription subscription = priceAlertRepository.findByUserIdAndPropertyId(current.getId(), propertyId)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        subscription.setActive(false);
        priceAlertRepository.save(subscription);
        return ResponseEntity.ok(Map.of("message", "unsubscribed"));
    }

    @PostMapping("/notify-test/{propertyId}")
    public ResponseEntity<?> notifyTest(@PathVariable Long propertyId) {
        Property property = propertyRepository.findById(propertyId).orElseThrow(() -> new RuntimeException("Property not found"));
        List<PriceAlertSubscription> subs = priceAlertRepository.findByPropertyIdAndActiveTrue(propertyId);
        User sender = property.getOwner() != null ? property.getOwner() : currentUser();

        for (PriceAlertSubscription sub : subs) {
            Message msg = new Message();
            msg.setSender(sender);
            msg.setReceiver(sub.getUser());
            msg.setProperty(property);
            msg.setContent("???????????: " + property.getTitle() + "????? " + property.getPrice() + " ?");
            messageRepository.save(msg);
        }

        return ResponseEntity.ok(Map.of("sent", subs.size()));
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(authentication.getName()).orElseThrow(() -> new RuntimeException("User not found"));
    }
}
