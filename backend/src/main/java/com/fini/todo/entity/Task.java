package com.fini.todo.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(
        name = "tasks",
        indexes = {
                @Index(name = "idx_tasks_user_notify", columnList = "user_id,notify_at,notify_time"),
                @Index(name = "idx_tasks_user_category", columnList = "user_id,category_id"),
                @Index(name = "idx_tasks_user_updated", columnList = "user_id,updated_at")
        }
)
public class Task {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(nullable = false)
    private Boolean completed = false;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean priority = false;

    @Column(name = "due_at")
    private LocalDateTime dueAt;

    @Column(name = "reminder_enabled", nullable = false)
    private Boolean reminderEnabled = false;


    @Column(name = "notify_at")
    private LocalDateTime notifyAt;

    @Column(name = "notify_time")
    private LocalTime notifyTime;

    @Column(name = "auto_trash_after_notification", nullable = false, columnDefinition = "boolean default false")
    private Boolean autoTrashAfterNotification = false;

    @Column(name = "repeat_type", nullable = false, length = 20)
    private String repeatType = "NONE";

    @Column(name = "repeat_days", length = 100)
    private String repeatDays;

    @Column(name = "has_location", nullable = false)
    private Boolean hasLocation = false;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "location_name")
    private String locationName;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "trashed_at")
    private LocalDateTime trashedAt;

    @Column(name = "purge_after")
    private LocalDateTime purgeAfter;

    @Column
    private Integer version = 1;

    @PrePersist
    public void prePersist() {
        if (version == null) {
            version = 1;
        }

        if (id == null) {
            id = UUID.randomUUID();
        }

        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;

        if (completed == null) completed = false;
        if (priority == null) priority = false;
        if (reminderEnabled == null) reminderEnabled = false;
        if (autoTrashAfterNotification == null) autoTrashAfterNotification = false;
        if (repeatType == null) repeatType = "NONE";
        if (hasLocation == null) hasLocation = false;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public Boolean getPriority() {
        return priority;
    }

    public void setPriority(Boolean priority) {
        this.priority = priority;
    }

    public LocalDateTime getDueAt() {
        return dueAt;
    }

    public void setDueAt(LocalDateTime dueAt) {
        this.dueAt = dueAt;
    }

    public Boolean getReminderEnabled() {
        return reminderEnabled;
    }

    public void setReminderEnabled(Boolean reminderEnabled) {
        this.reminderEnabled = reminderEnabled;
    }

    public LocalDateTime getNotifyAt() {
        return notifyAt;
    }

    public void setNotifyAt(LocalDateTime notifyAt) {
        this.notifyAt = notifyAt;
    }

    public LocalTime getNotifyTime() {
        return notifyTime;
    }

    public void setNotifyTime(LocalTime notifyTime) {
        this.notifyTime = notifyTime;
    }

    public Boolean getAutoTrashAfterNotification() {
        return autoTrashAfterNotification;
    }

    public void setAutoTrashAfterNotification(Boolean autoTrashAfterNotification) {
        this.autoTrashAfterNotification = autoTrashAfterNotification;
    }

    public String getRepeatType() {
        return repeatType;
    }

    public void setRepeatType(String repeatType) {
        this.repeatType = repeatType;
    }

    public String getRepeatDays() {
        return repeatDays;
    }

    public void setRepeatDays(String repeatDays) {
        this.repeatDays = repeatDays;
    }

    public Boolean getHasLocation() {
        return hasLocation;
    }

    public void setHasLocation(Boolean hasLocation) {
        this.hasLocation = hasLocation;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public LocalDateTime getTrashedAt() {
        return trashedAt;
    }

    public void setTrashedAt(LocalDateTime trashedAt) {
        this.trashedAt = trashedAt;
    }

    public LocalDateTime getPurgeAfter() {
        return purgeAfter;
    }

    public void setPurgeAfter(LocalDateTime purgeAfter) {
        this.purgeAfter = purgeAfter;
    }
}
