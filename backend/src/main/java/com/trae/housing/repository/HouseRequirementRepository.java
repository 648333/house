package com.trae.housing.repository;

import com.trae.housing.model.HouseRequirement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HouseRequirementRepository extends JpaRepository<HouseRequirement, Long> {
    List<HouseRequirement> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<HouseRequirement> findAllByOrderByCreatedAtDesc();
    long countByUserIdAndStatusNot(Long userId, HouseRequirement.Status status);
}
