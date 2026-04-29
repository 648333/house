package com.trae.housing.controller;

import com.trae.housing.model.Appointment;
import com.trae.housing.model.HouseRequirement;
import com.trae.housing.model.Message;
import com.trae.housing.model.PaymentOrder;
import com.trae.housing.model.Property;
import com.trae.housing.model.PropertyInteraction;
import com.trae.housing.model.Review;
import com.trae.housing.model.User;
import com.trae.housing.repository.AppointmentRepository;
import com.trae.housing.repository.HouseRequirementRepository;
import com.trae.housing.repository.MessageRepository;
import com.trae.housing.repository.PaymentOrderRepository;
import com.trae.housing.repository.PropertyInteractionRepository;
import com.trae.housing.repository.PropertyRepository;
import com.trae.housing.repository.ReviewRepository;
import com.trae.housing.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/stats")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class StatsController {

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private PaymentOrderRepository paymentOrderRepository;

    @Autowired
    private PropertyInteractionRepository propertyInteractionRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private HouseRequirementRepository houseRequirementRepository;

    @GetMapping("/dashboard")
    public Map<String, Object> getDashboardStats() {
        return buildAgentStats(getCurrentUser());
    }

    @GetMapping("/agent")
    public Map<String, Object> getAgentStats() {
        return buildAgentStats(getCurrentUser());
    }

    @GetMapping("/admin")
    public Map<String, Object> getAdminStats() {
        List<Property> properties = propertyRepository.findAll();
        List<User> users = userRepository.findAll();
        List<Appointment> appointments = appointmentRepository.findAll();
        List<Message> messages = messageRepository.findAll();
        List<PaymentOrder> payments = paymentOrderRepository.findAll();
        List<PropertyInteraction> interactions = propertyInteractionRepository.findAll();
        List<Review> reviews = reviewRepository.findAll();
        List<HouseRequirement> requirements = houseRequirementRepository.findAllByOrderByCreatedAtDesc();

        long pendingProperties = countPropertiesByStatus(properties, Property.Status.PENDING);
        long approvedProperties = countPropertiesByStatus(properties, Property.Status.APPROVED);
        long soldProperties = countPropertiesByStatus(properties, Property.Status.SOLD);
        long rejectedProperties = countPropertiesByStatus(properties, Property.Status.REJECTED);
        long agents = users.stream().filter(u -> u != null && u.getRole() == User.Role.AGENT).count();
        long admins = users.stream().filter(u -> u != null && u.getRole() == User.Role.ADMIN).count();
        long enabledUsers = users.stream().filter(User::isEnabled).count();
        long paidOrders = payments.stream().filter(order -> order.getStatus() == PaymentOrder.PaymentStatus.PAID).count();
        long pendingOrders = payments.stream().filter(order -> order.getStatus() == PaymentOrder.PaymentStatus.PAYING || order.getStatus() == PaymentOrder.PaymentStatus.CREATED).count();
        long failedOrders = payments.stream().filter(order -> order.getStatus() == PaymentOrder.PaymentStatus.FAILED || order.getStatus() == PaymentOrder.PaymentStatus.CLOSED).count();
        long inquiryCount = messages.size();
        long viewCount = interactions.stream().filter(item -> item.getActionType() == PropertyInteraction.ActionType.VIEW).count();
        long favoriteCount = interactions.stream().filter(item -> item.getActionType() == PropertyInteraction.ActionType.FAVORITE).count();
        long appointmentCount = interactions.stream().filter(item -> item.getActionType() == PropertyInteraction.ActionType.APPOINTMENT).count();
        double avgRating = roundTwo(reviews.stream()
                .filter(review -> review.getRating() != null)
                .mapToInt(Review::getRating)
                .average()
                .orElse(0));

        Map<String, Object> stats = new HashMap<>();
        stats.put("users", users.size());
        stats.put("enabledUsers", enabledUsers);
        stats.put("agents", agents);
        stats.put("admins", admins);
        stats.put("pendingProperties", pendingProperties);
        stats.put("approvedProperties", approvedProperties);
        stats.put("soldProperties", soldProperties);
        stats.put("rejectedProperties", rejectedProperties);
        stats.put("activationRate", percentage(enabledUsers, users.size()));
        stats.put("saleRate", percentage(soldProperties, properties.size()));
        stats.put("messagesTotal", inquiryCount);
        stats.put("appointmentsTotal", appointments.size());
        stats.put("reviewsTotal", reviews.size());
        stats.put("requirementsTotal", requirements.size());
        stats.put("paidOrders", paidOrders);
        stats.put("pendingOrders", pendingOrders);
        stats.put("failedOrders", failedOrders);
        stats.put("gmv", sumPaidAmount(payments));
        stats.put("averageRating", avgRating);
        stats.put("propertyStatus", Map.of(
                "pending", pendingProperties,
                "approved", approvedProperties,
                "sold", soldProperties,
                "rejected", rejectedProperties
        ));
        stats.put("roleDistribution", Map.of(
                "users", Math.max(0, users.size() - (int) agents - (int) admins),
                "agents", agents,
                "admins", admins
        ));
        stats.put("interactionFunnel", Map.of(
                "views", viewCount,
                "favorites", favoriteCount,
                "inquiries", inquiryCount,
                "appointments", appointmentCount,
                "paidOrders", paidOrders
        ));
        stats.put("appointmentStatus", countAppointmentsByStatus(appointments));
        stats.put("paymentStatus", countPaymentByStatus(payments));
        stats.put("requirementStatus", countRequirementByStatus(requirements));
        stats.put("recentActivity", buildRecentActivity(messages, appointments, payments));
        return stats;
    }

    private Map<String, Object> buildAgentStats(User currentUser) {
        List<Property> properties = propertyRepository.findByOwnerUsername(currentUser.getUsername());
        List<Appointment> appointments = appointmentRepository.findByPropertyOwnerId(currentUser.getId());
        List<PaymentOrder> payments = paymentOrderRepository.findAll().stream()
                .filter(order -> order.getProperty() != null
                        && order.getProperty().getOwner() != null
                        && currentUser.getId().equals(order.getProperty().getOwner().getId()))
                .toList();
        List<Message> messages = messageRepository.findByReceiverIdOrSenderId(currentUser.getId());
        List<HouseRequirement> requirements = houseRequirementRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(item -> item.getAssignedAgent() != null && currentUser.getId().equals(item.getAssignedAgent().getId()))
                .toList();

        Set<Long> propertyIds = properties.stream().map(Property::getId).collect(Collectors.toSet());
        List<PropertyInteraction> interactions = propertyInteractionRepository.findAll().stream()
                .filter(item -> item.getProperty() != null && propertyIds.contains(item.getProperty().getId()))
                .toList();
        List<Review> reviews = reviewRepository.findAll().stream()
                .filter(item -> item.getProperty() != null && propertyIds.contains(item.getProperty().getId()))
                .toList();

        long onSale = countPropertiesByStatus(properties, Property.Status.APPROVED);
        long sold = countPropertiesByStatus(properties, Property.Status.SOLD);
        long pending = countPropertiesByStatus(properties, Property.Status.PENDING);
        long rejected = countPropertiesByStatus(properties, Property.Status.REJECTED);
        long viewCount = interactions.stream().filter(item -> item.getActionType() == PropertyInteraction.ActionType.VIEW).count();
        long favoriteCount = interactions.stream().filter(item -> item.getActionType() == PropertyInteraction.ActionType.FAVORITE).count();
        long tourCount = interactions.stream().filter(item -> item.getActionType() == PropertyInteraction.ActionType.TOUR_OPEN).count();
        long appointmentIntentCount = interactions.stream().filter(item -> item.getActionType() == PropertyInteraction.ActionType.APPOINTMENT).count();
        long consultCount = messages.size();
        long unreadIncoming = messages.stream()
                .filter(item -> item.getReceiver() != null
                        && currentUser.getId().equals(item.getReceiver().getId())
                        && !item.isRead())
                .count();
        Set<Long> clientIds = new HashSet<>();
        messages.forEach(message -> {
            if (message.getSender() != null && !currentUser.getId().equals(message.getSender().getId())) {
                clientIds.add(message.getSender().getId());
            }
            if (message.getReceiver() != null && !currentUser.getId().equals(message.getReceiver().getId())) {
                clientIds.add(message.getReceiver().getId());
            }
        });

        long paidOrders = payments.stream().filter(order -> order.getStatus() == PaymentOrder.PaymentStatus.PAID).count();
        long activeOrders = payments.stream().filter(order -> order.getStatus() == PaymentOrder.PaymentStatus.PAYING || order.getStatus() == PaymentOrder.PaymentStatus.CREATED).count();
        long failedOrders = payments.stream().filter(order -> order.getStatus() == PaymentOrder.PaymentStatus.FAILED || order.getStatus() == PaymentOrder.PaymentStatus.CLOSED).count();
        double avgRating = roundTwo(reviews.stream()
                .filter(review -> review.getRating() != null)
                .mapToInt(Review::getRating)
                .average()
                .orElse(0));

        Map<String, Object> stats = new HashMap<>();
        stats.put("onSale", onSale);
        stats.put("sold", sold);
        stats.put("pending", pending);
        stats.put("views", viewCount);
        stats.put("consults", consultCount);
        stats.put("portfolio", Map.of(
                "total", properties.size(),
                "approved", onSale,
                "pending", pending,
                "sold", sold,
                "rejected", rejected
        ));
        stats.put("interactionFunnel", Map.of(
                "views", viewCount,
                "favorites", favoriteCount,
                "tourOpens", tourCount,
                "inquiries", consultCount,
                "appointments", appointmentIntentCount,
                "paidOrders", paidOrders
        ));
        stats.put("appointments", countAppointmentsByStatus(appointments));
        stats.put("payments", new HashMap<>(Map.of(
                "total", payments.size(),
                "paid", paidOrders,
                "paying", activeOrders,
                "failed", failedOrders,
                "paidAmount", sumPaidAmount(payments),
                "conversionRate", percentage(paidOrders, Math.max(consultCount, 1))
        )));
        stats.put("messages", Map.of(
                "total", consultCount,
                "unreadIncoming", unreadIncoming,
                "distinctClients", clientIds.size()
        ));
        stats.put("service", Map.of(
                "requirementsAssigned", requirements.size(),
                "openRequirements", requirements.stream().filter(item -> item.getStatus() != HouseRequirement.Status.CLOSED).count(),
                "averageRating", avgRating,
                "reviews", reviews.size()
        ));
        stats.put("recentActivity", buildRecentActivity(messages, appointments, payments));
        return stats;
    }

    private Map<String, Long> countAppointmentsByStatus(List<Appointment> appointments) {
        Map<String, Long> counts = new HashMap<>();
        counts.put("pending", appointments.stream().filter(item -> item.getStatus() == Appointment.Status.PENDING).count());
        counts.put("approved", appointments.stream().filter(item -> item.getStatus() == Appointment.Status.APPROVED).count());
        counts.put("completed", appointments.stream().filter(item -> item.getStatus() == Appointment.Status.COMPLETED).count());
        counts.put("rejected", appointments.stream().filter(item -> item.getStatus() == Appointment.Status.REJECTED).count());
        counts.put("cancelled", appointments.stream().filter(item -> item.getStatus() == Appointment.Status.CANCELLED).count());
        return counts;
    }

    private Map<String, Long> countPaymentByStatus(List<PaymentOrder> payments) {
        Map<String, Long> counts = new HashMap<>();
        counts.put("created", payments.stream().filter(item -> item.getStatus() == PaymentOrder.PaymentStatus.CREATED).count());
        counts.put("paying", payments.stream().filter(item -> item.getStatus() == PaymentOrder.PaymentStatus.PAYING).count());
        counts.put("paid", payments.stream().filter(item -> item.getStatus() == PaymentOrder.PaymentStatus.PAID).count());
        counts.put("failed", payments.stream().filter(item -> item.getStatus() == PaymentOrder.PaymentStatus.FAILED).count());
        counts.put("closed", payments.stream().filter(item -> item.getStatus() == PaymentOrder.PaymentStatus.CLOSED).count());
        return counts;
    }

    private Map<String, Long> countRequirementByStatus(List<HouseRequirement> requirements) {
        Map<String, Long> counts = new HashMap<>();
        for (HouseRequirement.Status status : HouseRequirement.Status.values()) {
            counts.put(status.name().toLowerCase(), requirements.stream().filter(item -> item.getStatus() == status).count());
        }
        return counts;
    }

    private Map<String, Object> buildRecentActivity(List<Message> messages, List<Appointment> appointments, List<PaymentOrder> payments) {
        LocalDate today = LocalDate.now();
        long recentMessages = messages.stream()
                .filter(item -> item.getSentAt() != null && !item.getSentAt().toLocalDate().isBefore(today.minusDays(6)))
                .count();
        long recentAppointments = appointments.stream()
                .filter(item -> item.getCreatedAt() != null && !item.getCreatedAt().toLocalDate().isBefore(today.minusDays(6)))
                .count();
        long recentPaidOrders = payments.stream()
                .filter(item -> item.getPaidAt() != null && !item.getPaidAt().toLocalDate().isBefore(today.minusDays(6)))
                .count();

        return Map.of(
                "messages7d", recentMessages,
                "appointments7d", recentAppointments,
                "paidOrders7d", recentPaidOrders
        );
    }

    private BigDecimal sumPaidAmount(List<PaymentOrder> payments) {
        return payments.stream()
                .filter(item -> item.getStatus() == PaymentOrder.PaymentStatus.PAID && item.getAmount() != null)
                .map(PaymentOrder::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private long countPropertiesByStatus(List<Property> properties, Property.Status status) {
        return properties.stream().filter(item -> item != null && item.getStatus() == status).count();
    }

    private double roundTwo(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private double percentage(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0;
        }
        return roundTwo((numerator * 100.0) / denominator);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        return userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
