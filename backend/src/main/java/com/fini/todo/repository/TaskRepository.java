package com.fini.todo.repository;

import com.fini.todo.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    Optional<Task> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

    Optional<Task> findByIdAndUserId(UUID id, UUID userId);

    Optional<Task> findByIdAndUserIdAndDeletedAtIsNullAndTrashedAtIsNull(UUID id, UUID userId);

    boolean existsByUserIdAndTitleAndDeletedAtIsNull(UUID userId, String title);

    Optional<Task> findByUserIdAndTitleAndDeletedAtIsNull(UUID userId, String title);

    List<Task> findByDeletedAtIsNullAndTrashedAtIsNotNullAndPurgeAfterBefore(LocalDateTime now);

    List<Task> findByDeletedAtBefore(LocalDateTime dateTime);

    List<Task> findByUserIdAndUpdatedAtAfterOrderByUpdatedAtAsc(
            UUID userId,
            LocalDateTime updatedAt
    );

    @Query("""
    SELECT t FROM Task t
    WHERE t.userId = :userId
      AND t.deletedAt IS NULL
      AND t.trashedAt IS NULL
      AND (:categoryId IS NULL OR t.categoryId = :categoryId)
    ORDER BY
      t.createdAt DESC
""")
    List<Task> searchTasksWithoutKeywordAndDate(
            @Param("userId") UUID userId,
            @Param("categoryId") UUID categoryId
    );

    @Query("""
    SELECT t FROM Task t
    WHERE t.userId = :userId
      AND t.deletedAt IS NULL
      AND t.trashedAt IS NULL
      AND (:categoryId IS NULL OR t.categoryId = :categoryId)
      AND (
            LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(COALESCE(t.note, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
      )
    ORDER BY
      t.createdAt DESC
""")
    List<Task> searchTasksWithKeywordAndDate(
            @Param("userId") UUID userId,
            @Param("categoryId") UUID categoryId,
            @Param("keyword") String keyword
    );

    @Query("""
    SELECT t FROM Task t
    WHERE t.userId = :userId
      AND t.deletedAt IS NULL
      AND t.trashedAt IS NULL
      AND (:categoryId IS NULL OR t.categoryId = :categoryId)
      AND t.notifyAt >= :fromTime
      AND t.notifyAt < :toTime
    ORDER BY
      t.notifyAt ASC,
      t.createdAt DESC
""")
    List<Task> searchTasksWithoutKeyword(
            @Param("userId") UUID userId,
            @Param("categoryId") UUID categoryId,
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime
    );

    @Query("""
    SELECT t FROM Task t
    WHERE t.userId = :userId
      AND t.deletedAt IS NULL
      AND t.trashedAt IS NULL
      AND (:categoryId IS NULL OR t.categoryId = :categoryId)
      AND (
            LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(COALESCE(t.note, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
      )
      AND t.notifyAt >= :fromTime
      AND t.notifyAt < :toTime
    ORDER BY
      t.notifyAt ASC,
      t.createdAt DESC
""")
    List<Task> searchTasksWithKeyword(
            @Param("userId") UUID userId,
            @Param("categoryId") UUID categoryId,
            @Param("keyword") String keyword,
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime
    );

    @Query("""
    SELECT t FROM Task t
    WHERE t.userId = :userId
      AND t.deletedAt IS NULL
      AND t.trashedAt IS NOT NULL
    ORDER BY t.trashedAt DESC
""")
    List<Task> findTrashByUserId(@Param("userId") UUID userId);
}
