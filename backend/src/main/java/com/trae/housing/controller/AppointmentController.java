package com.trae.housing.controller;

import com.trae.housing.model.Appointment;
import com.trae.housing.model.PaymentOrder;
import com.trae.housing.model.Property;
import com.trae.housing.model.User;
import com.trae.housing.repository.AppointmentRepository;
import com.trae.housing.repository.PaymentOrderRepository;
import com.trae.housing.repository.PropertyRepository;
import com.trae.housing.repository.UserRepository;
import com.trae.housing.service.InteractionTrackingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/appointments")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class AppointmentController {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private PaymentOrderRepository paymentOrderRepository;

    @Autowired
    private InteractionTrackingService interactionTrackingService;

    @PostMapping
    public ResponseEntity<?> createAppointment(@RequestBody Appointment appointmentRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Property property = propertyRepository.findById(appointmentRequest.getProperty().getId())
                .orElseThrow(() -> new RuntimeException("Property not found"));

        if (appointmentRequest.getAppointmentTime() == null) {
            return ResponseEntity.badRequest().body("appointmentTime is required");
        }

        boolean conflict = appointmentRepository.existsByPropertyIdAndAppointmentTimeAndStatusIn(
                property.getId(),
                appointmentRequest.getAppointmentTime(),
                List.of(Appointment.Status.PENDING, Appointment.Status.APPROVED)
        );
        if (conflict) {
            return ResponseEntity.badRequest().body("Selected time slot is already booked");
        }

        appointmentRequest.setUser(user);
        appointmentRequest.setProperty(property);
        appointmentRequest.setStatus(Appointment.Status.PENDING);

        Appointment saved = appointmentRepository.save(appointmentRequest);
        interactionTrackingService.track(user, property, InteractionTrackingService.ActionType.APPOINTMENT, "appointment-create", null, 4.0);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/my")
    public List<Appointment> getMyAppointments() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return appointmentRepository.findByUserId(user.getId());
    }

    @GetMapping("/agent")
    public List<Appointment> getAgentAppointments() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return appointmentRepository.findByPropertyOwnerId(user.getId());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestParam Appointment.Status status) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (!appointment.getProperty().getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body("Not authorized to update this appointment");
        }

        appointment.setStatus(status);
        return ResponseEntity.ok(appointmentRepository.save(appointment));
    }

    @GetMapping("/{id}/timeline")
    public ResponseEntity<?> getTimeline(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        boolean isOwner = appointment.getProperty().getOwner() != null
                && appointment.getProperty().getOwner().getId().equals(currentUser.getId());
        boolean isCreator = appointment.getUser().getId().equals(currentUser.getId());
        if (!isOwner && !isCreator && currentUser.getRole() != User.Role.ADMIN) {
            return ResponseEntity.status(403).body("Not authorized");
        }

        List<Map<String, Object>> timeline = new ArrayList<>();
        timeline.add(event("APPOINTMENT_CREATED", "已提交预约申请", appointment.getCreatedAt()));
        timeline.add(event("APPOINTMENT_STATUS", "预约状态: " + appointment.getStatus().name(), LocalDateTime.now()));

        Optional<PaymentOrder> paymentOrder = paymentOrderRepository.findTopByAppointmentIdOrderByCreatedAtDesc(appointment.getId());
        paymentOrder.ifPresent(order -> {
            timeline.add(event("PAYMENT_ORDER", "已创建支付订单", order.getCreatedAt()));
            timeline.add(event("PAYMENT_STATUS", "支付状态: " + order.getStatus().name(), order.getPaidAt() != null ? order.getPaidAt() : LocalDateTime.now()));
        });

        return ResponseEntity.ok(timeline);
    }

    private Map<String, Object> event(String code, String label, LocalDateTime time) {
        Map<String, Object> data = new HashMap<>();
        data.put("code", code);
        data.put("label", label);
        data.put("time", time);
        return data;
    }
}
