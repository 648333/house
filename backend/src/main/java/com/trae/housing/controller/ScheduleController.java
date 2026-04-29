package com.trae.housing.controller;

import com.trae.housing.model.AgentScheduleSlot;
import com.trae.housing.model.User;
import com.trae.housing.repository.AgentScheduleSlotRepository;
import com.trae.housing.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/schedules")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"}, allowCredentials = "true")
public class ScheduleController {

    @Autowired
    private AgentScheduleSlotRepository scheduleSlotRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/mine")
    public List<AgentScheduleSlot> mySlots() {
        User current = currentUser();
        return scheduleSlotRepository.findByAgentIdOrderByStartTimeAsc(current.getId());
    }

    @GetMapping("/agent/{agentId}")
    public List<AgentScheduleSlot> getAgentAvailableSlots(@PathVariable Long agentId) {
        return scheduleSlotRepository.findByAgentIdAndAvailableTrueAndStartTimeAfterOrderByStartTimeAsc(agentId, LocalDateTime.now());
    }

    @PostMapping
    public ResponseEntity<?> createSlot(@RequestBody AgentScheduleSlot request) {
        User current = currentUser();
        if (current.getRole() != User.Role.AGENT && current.getRole() != User.Role.ADMIN) {
            return ResponseEntity.status(403).body("Only agent/admin can manage schedule");
        }

        if (request.getStartTime() == null || request.getEndTime() == null || !request.getEndTime().isAfter(request.getStartTime())) {
            return ResponseEntity.badRequest().body("Invalid slot time");
        }

        request.setAgent(current);
        request.setAvailable(true);
        return ResponseEntity.ok(scheduleSlotRepository.save(request));
    }

    @PutMapping("/{id}/availability")
    public ResponseEntity<?> updateAvailability(@PathVariable Long id, @RequestParam boolean available) {
        User current = currentUser();
        AgentScheduleSlot slot = scheduleSlotRepository.findById(id).orElseThrow(() -> new RuntimeException("Slot not found"));
        if (!slot.getAgent().getId().equals(current.getId()) && current.getRole() != User.Role.ADMIN) {
            return ResponseEntity.status(403).body("Not authorized");
        }

        slot.setAvailable(available);
        return ResponseEntity.ok(scheduleSlotRepository.save(slot));
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(authentication.getName()).orElseThrow(() -> new RuntimeException("User not found"));
    }
}
