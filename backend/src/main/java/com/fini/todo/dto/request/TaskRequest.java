package com.fini.todo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public class TaskRequest {

    private UUID categoryId;

    @NotBlank(message = "Task title is required")
    @Size(max = 255, message = "Title must be less than 255 characters")
    private String title;

    private String note;

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
}
