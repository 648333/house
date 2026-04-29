package com.trae.housing.controller;

import com.trae.housing.model.Property;
import com.trae.housing.model.SupportTicket;
import com.trae.housing.model.User;
import com.trae.housing.repository.PropertyRepository;
import com.trae.housing.repository.SupportTicketRepository;
import com.trae.housing.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/support-tickets")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"}, allowCredentials = "true")
public class SupportTicketController {

    @Autowired
    private SupportTicketRepository supportTicketRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody SupportTicket request) {
        User current = currentUser();

        if (request == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "\u5de5\u5355\u5185\u5bb9\u4e0d\u80fd\u4e3a\u7a7a"));
        }

        String title = normalize(request.getTitle());
        String description = normalize(request.getDescription());
        String category = normalize(request.getCategory());

        if (title == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "\u8bf7\u586b\u5199\u5de5\u5355\u6807\u9898"));
        }
        if (description == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "\u8bf7\u586b\u5199\u95ee\u9898\u63cf\u8ff0"));
        }
        if (category == null) {
            category = "OTHER";
        }

        SupportTicket ticket = new SupportTicket();
        ticket.setUser(current);
        ticket.setCategory(category);
        ticket.setTitle(title);
        ticket.setDescription(description);
        ticket.setContactMobile(normalize(request.getContactMobile()));
        ticket.setPriority(request.getPriority() != null ? request.getPriority() : SupportTicket.Priority.MEDIUM);
        if (request.getProperty() != null && request.getProperty().getId() != null) {
            Property property = propertyRepository.findById(request.getProperty().getId())
                    .orElseThrow(() -> new RuntimeException("Property not found"));
            ticket.setProperty(property);
        }
        ticket.setStatus(SupportTicket.Status.OPEN);
        return ResponseEntity.ok(supportTicketRepository.save(ticket));
    }

    @GetMapping("/my")
    public List<SupportTicket> myTickets() {
        return supportTicketRepository.findByUserIdOrderByCreatedAtDesc(currentUser().getId());
    }

    @GetMapping
    public ResponseEntity<?> allTickets() {
        User current = currentUser();
        if (current.getRole() != User.Role.ADMIN && current.getRole() != User.Role.AGENT) {
            return ResponseEntity.status(403).body("Not authorized");
        }
        return ResponseEntity.ok(supportTicketRepository.findAllByOrderByCreatedAtDesc());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestParam SupportTicket.Status status,
            @RequestBody(required = false) Map<String, String> payload) {
        User current = currentUser();
        if (current.getRole() != User.Role.ADMIN && current.getRole() != User.Role.AGENT) {
            return ResponseEntity.status(403).body("Not authorized");
        }

        SupportTicket ticket = supportTicketRepository.findById(id).orElseThrow(() -> new RuntimeException("Ticket not found"));
        ticket.setStatus(status);
        if (payload != null) {
            String handlerNote = normalize(payload.get("handlerNote"));
            if (handlerNote != null) {
                ticket.setHandlerNote(handlerNote);
            }
        }
        return ResponseEntity.ok(supportTicketRepository.save(ticket));
    }

    @GetMapping("/categories")
    public List<Map<String, String>> categories() {
        return List.of(
                Map.of("value", "PROPERTY_INFO", "label", "\u623f\u6e90\u54a8\u8be2"),
                Map.of("value", "PAYMENT", "label", "\u4ea4\u6613\u652f\u4ed8"),
                Map.of("value", "APPOINTMENT", "label", "\u770b\u623f\u9884\u7ea6"),
                Map.of("value", "OTHER", "label", "\u5176\u4ed6\u95ee\u9898")
        );
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(authentication.getName()).orElseThrow(() -> new RuntimeException("User not found"));
    }
}
