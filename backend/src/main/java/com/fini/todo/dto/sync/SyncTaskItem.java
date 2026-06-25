package com.fini.todo.dto.sync;

import com.fini.todo.entity.Task;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public class SyncTaskItem {

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
    private Boolean deleted;
    private LocalDateTime trashedAt;
    private LocalDateTime purgeAfter;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SyncTaskItem from(Task task) {
        SyncTaskItem item = new SyncTaskItem();
        item.setId(task.getId());
        item.setCategoryId(task.getCategoryId());
        item.setTitle(task.getTitle());
        item.setNote(task.getNote());
        item.setCompleted(task.getCompleted());
        item.setPriority(task.getPriority());
        item.setDueAt(task.getDueAt());
        item.setReminderEnabled(task.getReminderEnabled());
        item.setNotifyAt(task.getNotifyAt());
        item.setNotifyTime(task.getNotifyTime());
        item.setAutoTrashAfterNotification(task.getAutoTrashAfterNotification());
        item.setRepeatType(task.getRepeatType());
        item.setRepeatDays(task.getRepeatDays());
        item.setHasLocation(task.getHasLocation());
        item.setLatitude(task.getLatitude());
        item.setLongitude(task.getLongitude());
        item.setLocationName(task.getLocationName());
        item.setAddress(task.getAddress());
        item.setDeleted(task.getDeletedAt() != null);
        item.setTrashedAt(task.getTrashedAt());
        item.setPurgeAfter(task.getPurgeAfter());
        item.setVersion(task.getVersion());
        item.setCreatedAt(task.getCreatedAt());
        item.setUpdatedAt(task.getUpdatedAt());
        return item;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
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
