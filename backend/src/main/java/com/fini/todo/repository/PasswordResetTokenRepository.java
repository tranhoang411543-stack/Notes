package com.fini.todo.repository;

import com.fini.todo.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findTopByUserIdAndUsedFalseOrderByCreatedAtDesc(UUID userId);

    List<PasswordResetToken> findByUserIdAndUsedFalse(UUID userId);
}