package com.trae.housing.repository;

import com.trae.housing.model.PropertyInteraction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PropertyInteractionRepository extends JpaRepository<PropertyInteraction, Long> {
    List<PropertyInteraction> findTop200ByUserIdOrderByCreatedAtDesc(Long userId);
    List<PropertyInteraction> findTop200ByPropertyIdOrderByCreatedAtDesc(Long propertyId);
}
