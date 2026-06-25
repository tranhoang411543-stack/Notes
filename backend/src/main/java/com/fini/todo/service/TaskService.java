package com.fini.todo.service;

import com.fini.todo.dto.request.TaskRequest;
import com.fini.todo.dto.response.TaskResponse;
import com.fini.todo.entity.Task;
import com.fini.todo.repository.CategoryRepository;
import com.fini.todo.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final CategoryRepository categoryRepository;

    public TaskService(
            TaskRepository taskRepository,
            CategoryRepository categoryRepository
    ) {
        this.taskRepository = taskRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<TaskResponse> getTasks(
            UUID userId,
            String dateFilter,
            UUID categoryId,
            String keyword
    ) {
        validateCategoryBelongsToUser(userId, categoryId);

        String normalizedFilter = dateFilter == null ? "ALL" : dateFilter.toUpperCase();
        if (!Set.of("ALL", "TODAY", "THIS_WEEK", "PRIORITY").contains(normalizedFilter)) {
            throw new RuntimeException("Invalid dateFilter. Use ALL, TODAY, THIS_WEEK, or PRIORITY");
        }

        String normalizedKeyword = normalizeKeyword(keyword);

        List<Task> tasks = normalizedKeyword == null
                ? taskRepository.searchTasksWithoutKeywordAndDate(userId, categoryId)
                : taskRepository.searchTasksWithKeywordAndDate(userId, categoryId, normalizedKeyword);

        LocalDate today = LocalDate.now();
        LocalDateTime fromTime = today.atStartOfDay();
        LocalDateTime tomorrow = today.plusDays(1).atStartOfDay();
        LocalDateTime nextMonday = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY)).atStartOfDay();

        Comparator<Task> createdDesc = Comparator
                .comparing(Task::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .reversed();
        Comparator<Task> notifyAsc = Comparator
                .<Task, LocalDateTime>comparing(
                        task -> nextNotificationAt(task, fromTime),
                        Comparator.nullsLast(Comparator.naturalOrder())
                )
                .thenComparing(createdDesc);

        if ("PRIORITY".equals(normalizedFilter)) {
            tasks = tasks.stream()
                    .filter(task -> Boolean.TRUE.equals(task.getPriority()))
                    .sorted(createdDesc)
                    .toList();
        } else if ("TODAY".equals(normalizedFilter)) {
            tasks = tasks.stream()
                    .filter(task -> isNotificationBetween(task, fromTime, tomorrow))
                    .sorted(notifyAsc)
                    .toList();
        } else if ("THIS_WEEK".equals(normalizedFilter)) {
            tasks = tasks.stream()
                    .filter(task -> isNotificationBetween(task, fromTime, nextMonday))
                    .sorted(notifyAsc)
                    .toList();
        } else {
            tasks = tasks.stream()
                    .sorted(createdDesc)
                    .toList();
        }

        return tasks.stream()
                .map(TaskResponse::from)
                .toList();
    }

    public TaskResponse getTaskById(UUID userId, UUID taskId) {
        Task task = taskRepository
                .findByIdAndUserId(taskId, userId)
                .filter(existing -> existing.getDeletedAt() == null)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        return TaskResponse.from(task);
    }

    public TaskResponse createTask(UUID userId, TaskRequest request) {
        validateTaskRequest(userId, request, null);

        Task task = new Task();
        task.setUserId(userId);

        applyRequestToTask(task, request);

        Task saved = taskRepository.save(task);
        return TaskResponse.from(saved);
    }

    public TaskResponse updateTask(UUID userId, UUID taskId, TaskRequest request) {
        Task task = taskRepository
                .findByIdAndUserId(taskId, userId)
                .filter(existing -> existing.getDeletedAt() == null)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        validateTaskRequest(userId, request, task.getNotifyAt());

        applyRequestToTask(task, request);

        task.setVersion(task.getVersion() == null ? 1 : task.getVersion() + 1);

        Task saved = taskRepository.save(task);
        return TaskResponse.from(saved);
    }


    public List<TaskResponse> getTrash(UUID userId) {
        return taskRepository.findTrashByUserId(userId)
                .stream()
                .map(TaskResponse::from)
                .toList();
    }

    @Transactional
    public void clearTrash(UUID userId) {
        List<Task> trash = taskRepository.findTrashByUserId(userId);
        if (!trash.isEmpty()) {
            taskRepository.deleteAllInBatch(trash);
        }
    }

    public TaskResponse trashTask(UUID userId, UUID taskId) {
        Task task = taskRepository
                .findByIdAndUserIdAndDeletedAtIsNullAndTrashedAtIsNull(taskId, userId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        moveToTrash(task);

        return TaskResponse.from(taskRepository.save(task));
    }

    public TaskResponse restoreTask(UUID userId, UUID taskId) {
        Task task = taskRepository
                .findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        task.setTrashedAt(null);
        task.setPurgeAfter(null);
        task.setDeletedAt(null);
        task.setVersion(task.getVersion() == null ? 1 : task.getVersion() + 1);

        return TaskResponse.from(taskRepository.save(task));
    }

    public TaskResponse setCompleted(UUID userId, UUID taskId, boolean completed) {
        Task task = taskRepository
                .findByIdAndUserIdAndDeletedAtIsNullAndTrashedAtIsNull(taskId, userId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        task.setCompleted(completed);

        task.setVersion(task.getVersion() == null ? 1 : task.getVersion() + 1);

        Task saved = taskRepository.save(task);
        return TaskResponse.from(saved);
    }

    private void applyRequestToTask(Task task, TaskRequest request) {
        String repeatType = TaskRules.normalizeRepeatType(request.getRepeatType());
        String repeatDays = TaskRules.normalizeRepeatDays(repeatType, request.getRepeatDays());
        boolean reminderEnabled = Boolean.TRUE.equals(request.getReminderEnabled());
        boolean hasLocation = Boolean.TRUE.equals(request.getHasLocation());
        LocalDateTime notifyAt = effectiveNotifyAt(repeatType, request.getNotifyAt(), request.getDueAt());
        LocalTime notifyTime = effectiveNotifyTime(repeatType, request.getNotifyTime(), request.getDueAt());
        boolean hasNotification = notifyAt != null || notifyTime != null;

        task.setCategoryId(request.getCategoryId());
        task.setTitle(TaskRules.trimRequired(request.getTitle(), "Task title is required"));
        task.setNote(TaskRules.trimToNull(request.getNote()));
        task.setPriority(Boolean.TRUE.equals(request.getPriority()));
        task.setNotifyAt(notifyAt);
        task.setNotifyTime(notifyTime);
        task.setAutoTrashAfterNotification(Boolean.TRUE.equals(request.getAutoTrashAfterNotification()));
        task.setDueAt(request.getDueAt() != null ? request.getDueAt() : notifyAt);

        task.setReminderEnabled(reminderEnabled || hasNotification);

        task.setRepeatType(repeatType);
        task.setRepeatDays(repeatDays);

        task.setHasLocation(hasLocation);

        if (hasLocation) {
            task.setLatitude(request.getLatitude());
            task.setLongitude(request.getLongitude());
            task.setLocationName(TaskRules.trimToNull(request.getLocationName()));
            task.setAddress(TaskRules.trimToNull(request.getAddress()));
        } else {
            task.setLatitude(null);
            task.setLongitude(null);
            task.setLocationName(null);
            task.setAddress(null);
        }
    }

    private void validateTaskRequest(UUID userId, TaskRequest request, LocalDateTime existingNotifyAt) {
        validateCategoryBelongsToUser(userId, request.getCategoryId());

        String repeatType = TaskRules.normalizeRepeatType(request.getRepeatType());
        LocalDateTime notifyAt = effectiveNotifyAt(repeatType, request.getNotifyAt(), request.getDueAt());
        LocalTime notifyTime = effectiveNotifyTime(repeatType, request.getNotifyTime(), request.getDueAt());

        TaskRules.validateNotification(repeatType, notifyAt, notifyTime, existingNotifyAt);
        TaskRules.normalizeRepeatDays(repeatType, request.getRepeatDays());
        TaskRules.validateLocation(
                request.getHasLocation(),
                request.getLatitude(),
                request.getLongitude()
        );
    }

    private void validateCategoryBelongsToUser(UUID userId, UUID categoryId) {
        if (categoryId == null) {
            return;
        }

        boolean exists = categoryRepository
                .findByIdAndUserIdAndDeletedAtIsNull(categoryId, userId)
                .isPresent();

        if (!exists) {
            throw new RuntimeException("Category not found");
        }
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        return keyword.trim();
    }

    private void moveToTrash(Task task) {
        LocalDateTime now = LocalDateTime.now();
        task.setTrashedAt(now);
        task.setPurgeAfter(now.plusDays(30));
    }

    private boolean isNotificationBetween(Task task, LocalDateTime fromInclusive, LocalDateTime toExclusive) {
        LocalDateTime notificationAt = nextNotificationAt(task, fromInclusive);
        return notificationAt != null &&
                !notificationAt.isBefore(fromInclusive) &&
                notificationAt.isBefore(toExclusive);
    }

    private LocalDateTime nextNotificationAt(Task task, LocalDateTime fromInclusive) {
        String repeatType = task.getRepeatType() == null ? "NONE" : task.getRepeatType().toUpperCase();
        if ("NONE".equals(repeatType)) {
            return task.getNotifyAt();
        }

        if (task.getDueAt() != null && !task.getDueAt().isBefore(fromInclusive)) {
            return task.getDueAt();
        }

        LocalTime time = task.getNotifyTime();
        if (time == null) {
            return null;
        }

        LocalDate cursor = fromInclusive.toLocalDate();
        for (int offset = 0; offset <= 7; offset++) {
            LocalDate candidateDate = cursor.plusDays(offset);
            if ("WEEKLY".equals(repeatType) && !repeatDayMatches(task.getRepeatDays(), candidateDate.getDayOfWeek())) {
                continue;
            }

            LocalDateTime candidate = candidateDate.atTime(time);
            if (!candidate.isBefore(fromInclusive)) {
                return candidate;
            }
        }

        return null;
    }

    private boolean repeatDayMatches(String repeatDays, DayOfWeek dayOfWeek) {
        if (repeatDays == null || repeatDays.isBlank()) {
            return false;
        }

        String target = switch (dayOfWeek) {
            case MONDAY -> "MON";
            case TUESDAY -> "TUE";
            case WEDNESDAY -> "WED";
            case THURSDAY -> "THU";
            case FRIDAY -> "FRI";
            case SATURDAY -> "SAT";
            case SUNDAY -> "SUN";
        };

        for (String token : repeatDays.split("[,\\s]+")) {
            if (target.equals(normalizeRepeatDayToken(token))) {
                return true;
            }
        }
        return false;
    }

    private String normalizeRepeatDayToken(String token) {
        return switch (token == null ? "" : token.trim().toUpperCase()) {
            case "1", "MON", "MONDAY" -> "MON";
            case "2", "TUE", "TUESDAY" -> "TUE";
            case "3", "WED", "WEDNESDAY" -> "WED";
            case "4", "THU", "THURSDAY" -> "THU";
            case "5", "FRI", "FRIDAY" -> "FRI";
            case "6", "SAT", "SATURDAY" -> "SAT";
            case "7", "SUN", "SUNDAY" -> "SUN";
            default -> "";
        };
    }

    private LocalDateTime effectiveNotifyAt(String repeatType, LocalDateTime notifyAt, LocalDateTime dueAt) {
        if ("NONE".equals(repeatType)) {
            return notifyAt != null ? notifyAt : dueAt;
        }
        return null;
    }

    private LocalTime effectiveNotifyTime(String repeatType, LocalTime notifyTime, LocalDateTime dueAt) {
        if ("NONE".equals(repeatType)) {
            return null;
        }
        if (notifyTime != null) {
            return notifyTime;
        }
        return dueAt == null ? null : dueAt.toLocalTime();
    }
}
