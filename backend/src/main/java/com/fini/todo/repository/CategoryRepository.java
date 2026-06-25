package com.fini.todo.repository;

import com.fini.todo.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID userId);

    Optional<Category> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

    Optional<Category> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndNameAndDeletedAtIsNull(UUID userId, String name);

    boolean existsByUserIdAndNameIgnoreCaseAndDeletedAtIsNull(UUID userId, String name);

    Optional<Category> findByUserIdAndNameIgnoreCaseAndDeletedAtIsNull(UUID userId, String name);

    List<Category> findByUserIdAndUpdatedAtAfterOrderByUpdatedAtAsc(
            UUID userId,
            LocalDateTime updatedAt
    );
}
