package com.fini.todo.dto.sync;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class SyncRequest {

    private UUID deviceId;
    private String deviceType;
    private String deviceName;
    private String fcmToken;
    private LocalDateTime lastSyncAt;
    private List<SyncCategoryItem> changedCategories;
    private List<SyncTaskItem> changedTasks;

    public UUID getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(UUID deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public LocalDateTime getLastSyncAt() {
        return lastSyncAt;
    }

    public void setLastSyncAt(LocalDateTime lastSyncAt) {
        this.lastSyncAt = lastSyncAt;
    }

    public List<SyncCategoryItem> getChangedCategories() {
        return changedCategories;
    }

    public void setChangedCategories(List<SyncCategoryItem> changedCategories) {
        this.changedCategories = changedCategories;
    }

    public List<SyncTaskItem> getChangedTasks() {
        return changedTasks;
    }

    public void setChangedTasks(List<SyncTaskItem> changedTasks) {
        this.changedTasks = changedTasks;
    }
}
