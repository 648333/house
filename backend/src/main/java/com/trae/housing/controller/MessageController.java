package com.trae.housing.controller;

import com.trae.housing.model.Message;
import com.trae.housing.model.Property;
import com.trae.housing.model.User;
import com.trae.housing.repository.MessageRepository;
import com.trae.housing.repository.PropertyRepository;
import com.trae.housing.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/messages")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class MessageController {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @GetMapping
    public List<Message> getMyMessages() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Get messages where I am sender or receiver
        return messageRepository.findByReceiverIdOrSenderId(user.getId());
    }

    @GetMapping("/my")
    public List<Message> getMessagesAlias() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Get messages where I am sender or receiver
        return messageRepository.findByReceiverIdOrSenderId(user.getId());
    }

    @GetMapping("/history")
    public List<Message> getChatHistory(@RequestParam Long propertyId, @RequestParam Long otherUserId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return messageRepository.findChatHistory(propertyId, currentUser.getId(), otherUserId);
    }

    @PostMapping
    public ResponseEntity<?> sendMessage(@RequestBody Message messageRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        User sender = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        User receiver = userRepository.findById(messageRequest.getReceiver().getId())
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        messageRequest.setSender(sender);
        messageRequest.setReceiver(receiver);

        if (messageRequest.getProperty() != null && messageRequest.getProperty().getId() != null) {
            Property property = propertyRepository.findById(messageRequest.getProperty().getId())
                    .orElse(null);
            messageRequest.setProperty(property);
        }
        
        return ResponseEntity.ok(messageRepository.save(messageRequest));
    }

    @PatchMapping("/{id}/read")
    @Transactional
    public ResponseEntity<?> markMessageAsRead(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Message message = messageRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));

        if (message.getReceiver() == null || !currentUser.getId().equals(message.getReceiver().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only receiver can mark this message as read");
        }

        if (!message.isRead()) {
            message.setRead(true);
            message = messageRepository.save(message);
        }

        return ResponseEntity.ok(message);
    }

    @PatchMapping("/read-all")
    @Transactional
    public ResponseEntity<?> markAllMyMessagesAsRead() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        int updated = messageRepository.markAllAsReadByReceiverId(currentUser.getId());
        return ResponseEntity.ok(Map.of("updated", updated));
    }

    @PatchMapping("/history/read")
    @Transactional
    public ResponseEntity<?> markChatHistoryAsRead(@RequestParam Long propertyId, @RequestParam Long otherUserId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        int updated = messageRepository.markChatHistoryAsRead(currentUser.getId(), otherUserId, propertyId);
        return ResponseEntity.ok(Map.of("updated", updated));
    }
}
