package com.fini.todo.dto.sync;

import com.fini.todo.entity.Category;
import java.time.LocalDateTime;
import java.util.UUID;

public class SyncCategoryItem {

    private UUID id;
    private String name;
    private String color;
    private Boolean deleted;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SyncCategoryItem from(Category category) {
        SyncCategoryItem item = new SyncCategoryItem();
        item.setId(category.getId());
        item.setName(category.getName());
        item.setColor(category.getColor());
        item.setDeleted(category.getDeletedAt() != null);
        item.setVersion(category.getVersion());
        item.setCreatedAt(category.getCreatedAt());
        item.setUpdatedAt(category.getUpdatedAt());
        return item;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
