package com.fini.todo.service;

import com.fini.todo.entity.Task;
import com.fini.todo.repository.TaskRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TaskTrashCleanupService {

    private final TaskRepository taskRepository;

    public TaskTrashCleanupService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpiredTrash() {
        // Step 1: Soft-delete expired trash tasks so client devices can sync the deletion
        List<Task> expiredTrash = taskRepository
                .findByDeletedAtIsNullAndTrashedAtIsNotNullAndPurgeAfterBefore(LocalDateTime.now());

        if (!expiredTrash.isEmpty()) {
            for (Task task : expiredTrash) {
                task.setDeletedAt(LocalDateTime.now());
                task.setVersion(task.getVersion() == null ? 1 : task.getVersion() + 1);
                taskRepository.save(task);
            }
        }

        // Step 2: Hard-delete tasks that have been soft-deleted for over 30 days
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        List<Task> expiredDeletes = taskRepository.findByDeletedAtBefore(cutoff);
        if (!expiredDeletes.isEmpty()) {
            taskRepository.deleteAllInBatch(expiredDeletes);
        }
    }
}
