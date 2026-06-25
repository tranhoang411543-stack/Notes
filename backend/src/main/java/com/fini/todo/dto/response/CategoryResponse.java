package com.fini.todo.dto.response;

import com.fini.todo.entity.Category;

import java.time.LocalDateTime;
import java.util.UUID;

public class CategoryResponse {

    private UUID id;
    private String name;
    private String color;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer version;

    public CategoryResponse(
            UUID id,
            String name,
            String color,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Integer version
    ) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getColor(),
                category.getCreatedAt(),
                category.getUpdatedAt(),
                category.getVersion()
        );
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Integer getVersion() {
        return version;
    }
}
