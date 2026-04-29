package com.trae.housing.repository;

import com.trae.housing.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findTopByEmailOrderByCreatedAtDesc(String email);
    Optional<PasswordResetToken> findTopByEmailAndCodeAndUsedAtIsNullOrderByCreatedAtDesc(String email, String code);
}

