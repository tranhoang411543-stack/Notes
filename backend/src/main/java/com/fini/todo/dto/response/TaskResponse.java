package com.fini.todo.dto.response;

import com.fini.todo.entity.Task;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public class TaskResponse {

    private UUID id;
    private UUID categoryId;
    private String title;
    private String note;
    private Boolean completed;
    private Boolean priority;
    private LocalDateTime dueAt;
    private Boolean reminderEnabled;
    private LocalDateTime notifyAt;
    private LocalTime notifyTime;
    private Boolean autoTrashAfterNotification;
    private String repeatType;
    private String repeatDays;
    private Boolean hasLocation;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String locationName;
    private String address;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime trashedAt;
    private LocalDateTime purgeAfter;
    private Integer version;

    public TaskResponse(
            UUID id,
            UUID categoryId,
            String title,
            String note,
            Boolean completed,
            Boolean priority,
            LocalDateTime dueAt,
            Boolean reminderEnabled,
            LocalDateTime notifyAt,
            LocalTime notifyTime,
            Boolean autoTrashAfterNotification,
            String repeatType,
            String repeatDays,
            Boolean hasLocation,
            BigDecimal latitude,
            BigDecimal longitude,
            String locationName,
            String address,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime trashedAt,
            LocalDateTime purgeAfter,
            Integer version
    ) {
        this.id = id;
        this.categoryId = categoryId;
        this.title = title;
        this.note = note;
        this.completed = completed;
        this.priority = priority;
        this.dueAt = dueAt;
        this.reminderEnabled = reminderEnabled;
        this.notifyAt = notifyAt;
        this.notifyTime = notifyTime;
        this.autoTrashAfterNotification = autoTrashAfterNotification;
        this.repeatType = repeatType;
        this.repeatDays = repeatDays;
        this.hasLocation = hasLocation;
        this.latitude = latitude;
        this.longitude = longitude;
        this.locationName = locationName;
        this.address = address;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.trashedAt = trashedAt;
        this.purgeAfter = purgeAfter;
        this.version = version;
    }

    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getCategoryId(),
                task.getTitle(),
                task.getNote(),
                task.getCompleted(),
                task.getPriority(),
                task.getDueAt(),
                task.getReminderEnabled(),
                task.getNotifyAt(),
                task.getNotifyTime(),
                task.getAutoTrashAfterNotification(),
                task.getRepeatType(),
                task.getRepeatDays(),
                task.getHasLocation(),
                task.getLatitude(),
                task.getLongitude(),
                task.getLocationName(),
                task.getAddress(),
                task.getCreatedAt(),
                task.getUpdatedAt(),
                task.getTrashedAt(),
                task.getPurgeAfter(),
                task.getVersion()
        );
    }

    public UUID getId() {
        return id;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public String getTitle() {
        return title;
    }

    public String getNote() {
        return note;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public Boolean getPriority() {
        return priority;
    }

    public Boolean getReminderEnabled() {
        return reminderEnabled;
    }

    public LocalDateTime getNotifyAt() {
        return notifyAt;
    }

    public LocalTime getNotifyTime() {
        return notifyTime;
    }

    public Boolean getAutoTrashAfterNotification() {
        return autoTrashAfterNotification;
    }

    public String getRepeatType() {
        return repeatType;
    }

    public String getRepeatDays() {
        return repeatDays;
    }

    public Boolean getHasLocation() {
        return hasLocation;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public String getLocationName() {
        return locationName;
    }

    public String getAddress() {
        return address;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getTrashedAt() {
        return trashedAt;
    }

    public LocalDateTime getPurgeAfter() {
        return purgeAfter;
    }

    public Integer getVersion() {
        return version;
    }
}
