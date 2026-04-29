package com.trae.housing.repository;

import com.trae.housing.model.PriceAlertSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PriceAlertSubscriptionRepository extends JpaRepository<PriceAlertSubscription, Long> {
    List<PriceAlertSubscription> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<PriceAlertSubscription> findByPropertyIdAndActiveTrue(Long propertyId);
    Optional<PriceAlertSubscription> findByUserIdAndPropertyId(Long userId, Long propertyId);
}
