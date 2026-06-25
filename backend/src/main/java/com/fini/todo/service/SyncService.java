package com.fini.todo.service;

import com.fini.todo.dto.sync.SyncCategoryItem;
import com.fini.todo.dto.sync.SyncRequest;
import com.fini.todo.dto.sync.SyncResponse;
import com.fini.todo.dto.sync.SyncTaskItem;
import com.fini.todo.entity.Category;
import com.fini.todo.entity.Device;
import com.fini.todo.entity.Task;
import com.fini.todo.repository.CategoryRepository;
import com.fini.todo.repository.DeviceRepository;
import com.fini.todo.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SyncService {

    private final DeviceRepository deviceRepository;
    private final CategoryRepository categoryRepository;
    private final TaskRepository taskRepository;

    public SyncService(
            DeviceRepository deviceRepository,
            CategoryRepository categoryRepository,
            TaskRepository taskRepository
    ) {
        this.deviceRepository = deviceRepository;
        this.categoryRepository = categoryRepository;
        this.taskRepository = taskRepository;
    }

    @Transactional
    public SyncResponse sync(UUID userId, SyncRequest request) {
        Device device = getOrCreateDevice(userId, request);
        applyCategoryChanges(userId, request);
        applyTaskChanges(userId, request);

        categoryRepository.flush();
        taskRepository.flush();

        LocalDateTime serverTime = LocalDateTime.now();
        LocalDateTime since = request.getLastSyncAt();
        if (since == null) {
            since = LocalDateTime.of(1970, 1, 1, 0, 0);
        }

        List<SyncCategoryItem> categories = categoryRepository
                .findByUserIdAndUpdatedAtAfterOrderByUpdatedAtAsc(userId, since)
                .stream()
                .map(SyncCategoryItem::from)
                .toList();

        List<SyncTaskItem> tasks = taskRepository
                .findByUserIdAndUpdatedAtAfterOrderByUpdatedAtAsc(userId, since)
                .stream()
                .map(SyncTaskItem::from)
                .toList();

        device.setLastSyncAt(serverTime);
        deviceRepository.save(device);

        return new SyncResponse(
                device.getId(),
                serverTime,
                categories,
                tasks
        );
    }

    private Device getOrCreateDevice(UUID userId, SyncRequest request) {
        if (request.getDeviceId() != null) {
            return deviceRepository
                    .findByIdAndUserId(request.getDeviceId(), userId)
                    .map(device -> updateDeviceInfo(device, request))
                    .orElseGet(() -> createDevice(userId, request));
        }

        return createDevice(userId, request);
    }

    private Device createDevice(UUID userId, SyncRequest request) {
        Device device = new Device();
        device.setUserId(userId);
        device.setDeviceType(normalizeDeviceType(request.getDeviceType()));
        device.setDeviceName(request.getDeviceName());
        device.setFcmToken(request.getFcmToken());
        return deviceRepository.save(device);
    }

    private Device updateDeviceInfo(Device device, SyncRequest request) {
        if (request.getDeviceType() != null) {
            device.setDeviceType(normalizeDeviceType(request.getDeviceType()));
        }

        if (request.getDeviceName() != null) {
            device.setDeviceName(request.getDeviceName());
        }

        if (request.getFcmToken() != null) {
            device.setFcmToken(request.getFcmToken());
        }

        return deviceRepository.save(device);
    }

    private String normalizeDeviceType(String deviceType) {
        if (deviceType == null || deviceType.isBlank()) {
            return "PHONE";
        }

        String normalized = deviceType.toUpperCase();

        if (!normalized.equals("PHONE") && !normalized.equals("CAR")) {
            throw new RuntimeException("deviceType must be PHONE or CAR");
        }

        return normalized;
    }

    private void applyCategoryChanges(UUID userId, SyncRequest request) {
        if (request.getChangedCategories() == null) {
            return;
        }

        for (SyncCategoryItem item : request.getChangedCategories()) {
            if (item.getId() == null) {
                item.setId(UUID.randomUUID());
            }

            Optional<Category> existingCategory = categoryRepository.findByIdAndUserId(item.getId(), userId);
            boolean deleted = Boolean.TRUE.equals(item.getDeleted());

            if (deleted && existingCategory.isEmpty()) {
                continue;
            }

            Category category = existingCategory.orElseGet(Category::new);

            category.setId(item.getId());
            category.setUserId(userId);

            if (deleted) {
                category.setDeletedAt(LocalDateTime.now());
            } else {
                String name = TaskRules.trimRequired(item.getName(), "Category name is required");
                validateCategoryNameAvailable(userId, item.getId(), name);

                category.setName(name);
                category.setColor(TaskRules.trimToNull(item.getColor()));
                category.setDeletedAt(null);
            }

            incrementCategoryVersion(category);
            categoryRepository.save(category);
        }
    }

    private void applyTaskChanges(UUID userId, SyncRequest request) {
        if (request.getChangedTasks() == null) {
            return;
        }

        for (SyncTaskItem item : request.getChangedTasks()) {
            if (item.getId() == null) {
                item.setId(UUID.randomUUID());
            }

            Optional<Task> existingTask = taskRepository.findByIdAndUserId(item.getId(), userId);
            boolean deleted = Boolean.TRUE.equals(item.getDeleted());

            if (deleted && existingTask.isEmpty()) {
                continue;
            }

            Task task = existingTask.orElseGet(Task::new);

            task.setId(item.getId());
            task.setUserId(userId);

            if (deleted) {
                task.setDeletedAt(LocalDateTime.now());
            } else {
                validateTaskSyncItem(userId, item, existingTask.map(Task::getNotifyAt).orElse(null));

                task.setCategoryId(item.getCategoryId());
                task.setTitle(TaskRules.trimRequired(item.getTitle(), "Task title is required"));
                task.setNote(TaskRules.trimToNull(item.getNote()));
                task.setCompleted(Boolean.TRUE.equals(item.getCompleted()));
                task.setPriority(Boolean.TRUE.equals(item.getPriority()));
                String repeatType = TaskRules.normalizeRepeatType(item.getRepeatType());
                String repeatDays = TaskRules.normalizeRepeatDays(repeatType, item.getRepeatDays());
                LocalDateTime notifyAt = effectiveNotifyAt(repeatType, item.getNotifyAt(), item.getDueAt());

                task.setNotifyAt(notifyAt);
                task.setNotifyTime(effectiveNotifyTime(repeatType, item.getNotifyTime(), item.getDueAt()));
                task.setAutoTrashAfterNotification(Boolean.TRUE.equals(item.getAutoTrashAfterNotification()));
                task.setDueAt(item.getDueAt() != null ? item.getDueAt() : notifyAt);

                boolean reminderEnabled = Boolean.TRUE.equals(item.getReminderEnabled()) ||
                        item.getNotifyAt() != null ||
                        item.getNotifyTime() != null;
                task.setReminderEnabled(reminderEnabled);

                task.setRepeatType(repeatType);
                task.setRepeatDays(repeatDays);

                boolean hasLocation = Boolean.TRUE.equals(item.getHasLocation());
                task.setHasLocation(hasLocation);

                if (hasLocation) {
                    task.setLatitude(item.getLatitude());
                    task.setLongitude(item.getLongitude());
                    task.setLocationName(TaskRules.trimToNull(item.getLocationName()));
                    task.setAddress(TaskRules.trimToNull(item.getAddress()));
                } else {
                    task.setLatitude(null);
                    task.setLongitude(null);
                    task.setLocationName(null);
                    task.setAddress(null);
                }

                task.setDeletedAt(null);
                task.setTrashedAt(item.getTrashedAt());
                task.setPurgeAfter(item.getPurgeAfter());
            }

            incrementTaskVersion(task);
            taskRepository.save(task);
        }
    }

    private void validateTaskSyncItem(UUID userId, SyncTaskItem item, LocalDateTime existingNotifyAt) {
        if (item.getTitle() == null || item.getTitle().isBlank()) {
            throw new RuntimeException("Task title is required");
        }

        if (item.getCategoryId() != null) {
            boolean categoryExists = categoryRepository
                    .findByIdAndUserIdAndDeletedAtIsNull(item.getCategoryId(), userId)
                    .isPresent();

            if (!categoryExists) {
                throw new RuntimeException("Category not found");
            }
        }

        String repeatType = TaskRules.normalizeRepeatType(item.getRepeatType());
        LocalDateTime notifyAt = effectiveNotifyAt(repeatType, item.getNotifyAt(), item.getDueAt());
        java.time.LocalTime notifyTime = effectiveNotifyTime(repeatType, item.getNotifyTime(), item.getDueAt());

        TaskRules.validateNotification(repeatType, notifyAt, notifyTime, existingNotifyAt);
        TaskRules.normalizeRepeatDays(repeatType, item.getRepeatDays());
        TaskRules.validateLocation(item.getHasLocation(), item.getLatitude(), item.getLongitude());
    }

    private void validateCategoryNameAvailable(UUID userId, UUID categoryId, String name) {
        categoryRepository
                .findByUserIdAndNameIgnoreCaseAndDeletedAtIsNull(userId, name)
                .filter(existing -> !existing.getId().equals(categoryId))
                .ifPresent(existing -> {
                    throw new RuntimeException("Category name already exists");
                });
    }

    private void incrementCategoryVersion(Category category) {
        Integer currentVersion = category.getVersion();
        category.setVersion(currentVersion == null ? 1 : currentVersion + 1);
    }

    private void incrementTaskVersion(Task task) {
        Integer currentVersion = task.getVersion();
        task.setVersion(currentVersion == null ? 1 : currentVersion + 1);
    }

    private LocalDateTime effectiveNotifyAt(String repeatType, LocalDateTime notifyAt, LocalDateTime dueAt) {
        if ("NONE".equals(repeatType)) {
            return notifyAt != null ? notifyAt : dueAt;
        }
        return null;
    }

    private java.time.LocalTime effectiveNotifyTime(String repeatType, java.time.LocalTime notifyTime, LocalDateTime dueAt) {
        if ("NONE".equals(repeatType)) {
            return null;
        }
        if (notifyTime != null) {
            return notifyTime;
        }
        return dueAt == null ? null : dueAt.toLocalTime();
    }
}
