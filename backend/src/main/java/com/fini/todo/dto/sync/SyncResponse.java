package com.fini.todo.dto.sync;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class SyncResponse {

    private UUID deviceId;
    private LocalDateTime serverTime;
    private List<SyncCategoryItem> categories;
    private List<SyncTaskItem> tasks;

    public SyncResponse() {
    }

    public SyncResponse(UUID deviceId, LocalDateTime serverTime, List<SyncCategoryItem> categories, List<SyncTaskItem> tasks) {
        this.deviceId = deviceId;
        this.serverTime = serverTime;
        this.categories = categories;
        this.tasks = tasks;
    }

    public UUID getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(UUID deviceId) {
        this.deviceId = deviceId;
    }

    public LocalDateTime getServerTime() {
        return serverTime;
    }

    public void setServerTime(LocalDateTime serverTime) {
        this.serverTime = serverTime;
    }

    public List<SyncCategoryItem> getCategories() {
        return categories;
    }

    public void setCategories(List<SyncCategoryItem> categories) {
        this.categories = categories;
    }

    public List<SyncTaskItem> getTasks() {
        return tasks;
    }

    public void setTasks(List<SyncTaskItem> tasks) {
        this.tasks = tasks;
    }
}
