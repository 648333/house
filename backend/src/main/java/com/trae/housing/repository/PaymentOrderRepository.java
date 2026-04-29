package com.trae.housing.repository;

import com.trae.housing.model.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {
    Optional<PaymentOrder> findByOutTradeNo(String outTradeNo);
    List<PaymentOrder> findByPayerIdOrderByCreatedAtDesc(Long payerId);
    Optional<PaymentOrder> findTopByAppointmentIdOrderByCreatedAtDesc(Long appointmentId);
}
