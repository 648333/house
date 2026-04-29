package com.trae.housing.controller;

import com.trae.housing.model.Appointment;
import com.trae.housing.model.PaymentOrder;
import com.trae.housing.model.Property;
import com.trae.housing.model.User;
import com.trae.housing.repository.AppointmentRepository;
import com.trae.housing.repository.PaymentOrderRepository;
import com.trae.housing.repository.PropertyRepository;
import com.trae.housing.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/payments")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"}, allowCredentials = "true")
public class PaymentController {

    @Autowired
    private PaymentOrderRepository paymentOrderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Value("${payment.mock.signature-secret:trae-demo-secret}")
    private String signatureSecret;
    
    @Value("${payment.mock.force-success:true}")
    private boolean forceSuccess;

    @PostMapping("/orders")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> payload) {
        User currentUser = getCurrentUser();

        Long propertyId = parseLong(payload.get("propertyId"));
        if (propertyId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "propertyId is required"));
        }

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new RuntimeException("Property not found"));

        Long appointmentId = parseLong(payload.get("appointmentId"));
        Appointment appointment = null;
        if (appointmentId != null) {
            appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new RuntimeException("Appointment not found"));
            if (!appointment.getUser().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(403).body(Map.of("message", "Not authorized for this appointment"));
            }
            if (!appointment.getProperty().getId().equals(property.getId())) {
                return ResponseEntity.badRequest().body(Map.of("message", "Appointment and property mismatch"));
            }

            Optional<PaymentOrder> existing = paymentOrderRepository.findTopByAppointmentIdOrderByCreatedAtDesc(appointmentId);
            if (existing.isPresent() && existing.get().getStatus() == PaymentOrder.PaymentStatus.PAID) {
                return ResponseEntity.badRequest().body(Map.of("message", "This appointment is already paid"));
            }
        }

        PaymentOrder.PaymentChannel channel = parseChannel((String) payload.get("channel"));
        if (channel == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Unsupported payment channel"));
        }

        BigDecimal amount = parseAmount(payload.get("amount"));
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            amount = suggestDeposit(property.getPrice());
        }

        PaymentOrder order = new PaymentOrder();
        order.setOutTradeNo(generateOutTradeNo());
        order.setPayer(currentUser);
        order.setProperty(property);
        order.setAppointment(appointment);
        order.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
        order.setChannel(channel);
        order.setStatus(PaymentOrder.PaymentStatus.PAYING);
        order.setSubject("Booking Deposit - " + property.getTitle());
        order.setExpireAt(LocalDateTime.now().plusMinutes(15));
        order.setQrCodeUrl("https://pay.mock.local/qr/" + order.getOutTradeNo() + "?channel=" + channel.name().toLowerCase(Locale.ROOT));

        PaymentOrder saved = paymentOrderRepository.save(order);
        return ResponseEntity.ok(toOrderView(saved));
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<?> getOrder(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        PaymentOrder order = paymentOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getPayer().getId().equals(currentUser.getId())
                && (order.getProperty().getOwner() == null || !order.getProperty().getOwner().getId().equals(currentUser.getId()))
                && currentUser.getRole() != User.Role.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("message", "Not authorized"));
        }

        return ResponseEntity.ok(toOrderView(order));
    }

    @GetMapping("/my")
    public List<Map<String, Object>> getMyOrders() {
        User currentUser = getCurrentUser();
        return paymentOrderRepository.findByPayerIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(this::toOrderView)
                .toList();
    }

    @GetMapping("/orders/{id}/voucher")
    public ResponseEntity<?> getVoucher(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        PaymentOrder order = paymentOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getPayer().getId().equals(currentUser.getId())
                && currentUser.getRole() != User.Role.ADMIN
                && (order.getProperty().getOwner() == null || !order.getProperty().getOwner().getId().equals(currentUser.getId()))) {
            return ResponseEntity.status(403).body(Map.of("message", "Not authorized"));
        }

        if (order.getStatus() != PaymentOrder.PaymentStatus.PAID) {
            return ResponseEntity.badRequest().body(Map.of("message", "Order is not paid yet"));
        }

        Map<String, Object> voucher = new HashMap<>();
        voucher.put("voucherNo", "V" + order.getOutTradeNo());
        voucher.put("customer", order.getPayer().getUsername());
        voucher.put("propertyTitle", order.getProperty().getTitle());
        voucher.put("amount", order.getAmount());
        voucher.put("paidAt", order.getPaidAt());
        voucher.put("summary", "预约意向金支付成功，可凭此凭证联系顾问安排看房");
        voucher.put("smsTemplate", buildSmsTemplate(order));
        return ResponseEntity.ok(voucher);
    }

    @PostMapping("/orders/{id}/pay")
    public ResponseEntity<?> payOrder(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> payload) {
        User currentUser = getCurrentUser();
        PaymentOrder order = paymentOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getPayer().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(403).body(Map.of("message", "Only payer can pay this order"));
        }

        if (order.getStatus() == PaymentOrder.PaymentStatus.PAID) {
            return ResponseEntity.ok(toOrderView(order));
        }

        if (!forceSuccess && order.getExpireAt() != null && LocalDateTime.now().isAfter(order.getExpireAt())) {
            order.setStatus(PaymentOrder.PaymentStatus.CLOSED);
            order.setFailureReason("Order expired");
            return ResponseEntity.ok(toOrderView(paymentOrderRepository.save(order)));
        }

        boolean requestSuccess = payload == null || !Boolean.FALSE.equals(payload.get("success"));
        if (!forceSuccess && !requestSuccess) {
            order.setStatus(PaymentOrder.PaymentStatus.FAILED);
            order.setFailureReason("Payment failed, please retry");
            return ResponseEntity.ok(toOrderView(paymentOrderRepository.save(order)));
        }

        markOrderPaid(order);
        return ResponseEntity.ok(toOrderView(paymentOrderRepository.save(order)));
    }

    @PostMapping("/orders/{id}/mock-success")
    public ResponseEntity<?> mockSuccess(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        PaymentOrder order = paymentOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getPayer().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(403).body(Map.of("message", "Only payer can complete this order"));
        }

        markOrderPaid(order);
        return ResponseEntity.ok(toOrderView(paymentOrderRepository.save(order)));
    }

    @PostMapping("/notify/mock")
    public ResponseEntity<?> mockNotify(@RequestBody Map<String, Object> payload) {
        String outTradeNo = (String) payload.get("outTradeNo");
        String status = (String) payload.get("status");
        String signature = (String) payload.get("signature");

        if (outTradeNo == null || status == null || signature == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "outTradeNo, status and signature are required"));
        }

        String expected = sign(outTradeNo, status);
        if (!expected.equalsIgnoreCase(signature)) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid signature"));
        }

        PaymentOrder order = paymentOrderRepository.findByOutTradeNo(outTradeNo)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if ("PAID".equalsIgnoreCase(status)) {
            markOrderPaid(order);
        } else if ("FAILED".equalsIgnoreCase(status)) {
            order.setStatus(PaymentOrder.PaymentStatus.FAILED);
            order.setFailureReason("Third-party callback: failed");
        } else if ("CLOSED".equalsIgnoreCase(status)) {
            order.setStatus(PaymentOrder.PaymentStatus.CLOSED);
            order.setFailureReason("Third-party callback: closed");
        } else {
            return ResponseEntity.badRequest().body(Map.of("message", "Unsupported status"));
        }

        return ResponseEntity.ok(toOrderView(paymentOrderRepository.save(order)));
    }

    private void markOrderPaid(PaymentOrder order) {
        order.setStatus(PaymentOrder.PaymentStatus.PAID);
        order.setPaidAt(LocalDateTime.now());
        order.setFailureReason(null);
        order.setThirdPartyTradeNo("MOCK" + System.currentTimeMillis());

        Appointment appointment = order.getAppointment();
        if (appointment != null && appointment.getStatus() == Appointment.Status.PENDING) {
            appointment.setStatus(Appointment.Status.APPROVED);
            appointmentRepository.save(appointment);
        }
    }

    private Map<String, Object> toOrderView(PaymentOrder order) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", order.getId());
        data.put("outTradeNo", order.getOutTradeNo());
        data.put("subject", order.getSubject());
        data.put("amount", order.getAmount());
        data.put("status", order.getStatus());
        data.put("channel", order.getChannel());
        data.put("qrCodeUrl", order.getQrCodeUrl());
        data.put("thirdPartyTradeNo", order.getThirdPartyTradeNo());
        data.put("failureReason", order.getFailureReason());
        data.put("expireAt", order.getExpireAt());
        data.put("paidAt", order.getPaidAt());
        data.put("createdAt", order.getCreatedAt());
        data.put("voucherAvailable", order.getStatus() == PaymentOrder.PaymentStatus.PAID);
        data.put("smsTemplate", buildSmsTemplate(order));

        if (order.getProperty() != null) {
            data.put("propertyId", order.getProperty().getId());
            data.put("propertyTitle", order.getProperty().getTitle());
        }

        if (order.getAppointment() != null) {
            data.put("appointmentId", order.getAppointment().getId());
            data.put("appointmentStatus", order.getAppointment().getStatus());
        }

        return data;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        return userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private String generateOutTradeNo() {
        String time = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        int random = ThreadLocalRandom.current().nextInt(100000, 999999);
        return "PM" + time + random;
    }

    private PaymentOrder.PaymentChannel parseChannel(String channel) {
        if (channel == null) {
            return PaymentOrder.PaymentChannel.WECHAT;
        }

        try {
            return PaymentOrder.PaymentChannel.valueOf(channel.toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            return null;
        }
    }

    private BigDecimal parseAmount(Object value) {
        if (value == null) {
            return null;
        }

        try {
            return new BigDecimal(String.valueOf(value));
        } catch (Exception ex) {
            return null;
        }
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }

        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ex) {
            return null;
        }
    }

    private BigDecimal suggestDeposit(BigDecimal propertyPriceWan) {
        BigDecimal base = propertyPriceWan.multiply(new BigDecimal("10000"));
        BigDecimal twoPercent = base.multiply(new BigDecimal("0.02"));
        BigDecimal min = new BigDecimal("1000");
        BigDecimal max = new BigDecimal("50000");

        if (twoPercent.compareTo(min) < 0) {
            return min;
        }

        if (twoPercent.compareTo(max) > 0) {
            return max;
        }

        return twoPercent;
    }

    private String sign(String outTradeNo, String status) {
        String source = outTradeNo + ":" + status + ":" + signatureSecret;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new RuntimeException("Unable to sign callback", ex);
        }
    }

    private String buildSmsTemplate(PaymentOrder order) {
        String payer = order.getPayer() != null ? order.getPayer().getUsername() : "客户";
        String propertyTitle = order.getProperty() != null ? order.getProperty().getTitle() : "房源";
        return "【暖寓找房】" + payer + "，你支付的 " + propertyTitle + " 预约意向金 ¥" + order.getAmount()
                + " 已" + (order.getStatus() == PaymentOrder.PaymentStatus.PAID ? "成功" : "提交")
                + "，订单号 " + order.getOutTradeNo() + "。";
    }
}
