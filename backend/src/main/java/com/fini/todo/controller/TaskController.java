package com.fini.todo.controller;

import com.fini.todo.dto.request.TaskRequest;
import com.fini.todo.dto.response.TaskResponse;
import com.fini.todo.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public List<TaskResponse> getTasks(
            Authentication authentication,
            @RequestParam(defaultValue = "ALL") String dateFilter,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String keyword
    ) {
        UUID userId = (UUID) authentication.getPrincipal();
        return taskService.getTasks(userId, dateFilter, categoryId, keyword);
    }

    @GetMapping("/{id}")
    public TaskResponse getTaskById(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        UUID userId = (UUID) authentication.getPrincipal();
        return taskService.getTaskById(userId, id);
    }

    @GetMapping("/trash")
    public List<TaskResponse> getTrash(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return taskService.getTrash(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse createTask(
            Authentication authentication,
            @Valid @RequestBody TaskRequest request
    ) {
        UUID userId = (UUID) authentication.getPrincipal();
        return taskService.createTask(userId, request);
    }

    @PutMapping("/{id}")
    public TaskResponse updateTask(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody TaskRequest request
    ) {
        UUID userId = (UUID) authentication.getPrincipal();
        return taskService.updateTask(userId, id, request);
    }

    @DeleteMapping("/trash")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearTrash(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        taskService.clearTrash(userId);
    }

    @PatchMapping("/{id}/trash")
    public TaskResponse trashTask(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        UUID userId = (UUID) authentication.getPrincipal();
        return taskService.trashTask(userId, id);
    }

    @PatchMapping("/{id}/restore")
    public TaskResponse restoreTask(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        UUID userId = (UUID) authentication.getPrincipal();
        return taskService.restoreTask(userId, id);
    }

    @PatchMapping("/{id}/complete")
    public TaskResponse setCompleted(
            Authentication authentication,
            @PathVariable UUID id,
            @RequestParam(defaultValue = "true") boolean completed
    ) {
        UUID userId = (UUID) authentication.getPrincipal();
        return taskService.setCompleted(userId, id, completed);
    }
}
