package com.fini.todo.controller;

import com.fini.todo.dto.sync.SyncRequest;
import com.fini.todo.dto.sync.SyncResponse;
import com.fini.todo.service.SyncService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/sync")
public class SyncController {

    private final SyncService syncService;

    public SyncController(SyncService syncService) {
        this.syncService = syncService;
    }

    @PostMapping
    public SyncResponse sync(
            Authentication authentication,
            @Valid @RequestBody SyncRequest request
    ) {
        UUID userId = (UUID) authentication.getPrincipal();
        return syncService.sync(userId, request);
    }
}
