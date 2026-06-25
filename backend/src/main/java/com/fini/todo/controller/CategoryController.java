package com.fini.todo.controller;

import com.fini.todo.dto.request.CategoryRequest;
import com.fini.todo.dto.response.CategoryResponse;
import com.fini.todo.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<CategoryResponse> getCategories(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return categoryService.getCategories(userId);
    }

    @GetMapping("/{id}")
    public CategoryResponse getCategoryById(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        UUID userId = (UUID) authentication.getPrincipal();
        return categoryService.getCategoryById(userId, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse createCategory(
            Authentication authentication,
            @Valid @RequestBody CategoryRequest request
    ) {
        UUID userId = (UUID) authentication.getPrincipal();
        return categoryService.createCategory(userId, request);
    }

    @PutMapping("/{id}")
    public CategoryResponse updateCategory(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody CategoryRequest request
    ) {
        UUID userId = (UUID) authentication.getPrincipal();
        return categoryService.updateCategory(userId, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        UUID userId = (UUID) authentication.getPrincipal();
        categoryService.deleteCategory(userId, id);
    }
}
