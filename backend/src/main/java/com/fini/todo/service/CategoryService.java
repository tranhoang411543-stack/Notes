package com.fini.todo.service;

import com.fini.todo.dto.request.CategoryRequest;
import com.fini.todo.dto.response.CategoryResponse;
import com.fini.todo.entity.Category;
import com.fini.todo.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<CategoryResponse> getCategories(UUID userId) {
        return categoryRepository
                .findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId)
                .stream()
                .map(CategoryResponse::from)
                .toList();
    }

    public CategoryResponse getCategoryById(UUID userId, UUID categoryId) {
        Category category = categoryRepository
                .findByIdAndUserIdAndDeletedAtIsNull(categoryId, userId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        return CategoryResponse.from(category);
    }

    public CategoryResponse createCategory(UUID userId, CategoryRequest request) {
        String name = TaskRules.trimRequired(request.getName(), "Category name is required");

        if (categoryRepository.existsByUserIdAndNameIgnoreCaseAndDeletedAtIsNull(userId, name)) {
            throw new RuntimeException("Category name already exists");
        }

        Category category = new Category();
        category.setUserId(userId);
        category.setName(name);
        category.setColor(TaskRules.trimToNull(request.getColor()));

        Category saved = categoryRepository.save(category);

        return CategoryResponse.from(saved);
    }

    public CategoryResponse updateCategory(UUID userId, UUID categoryId, CategoryRequest request) {
        Category category = categoryRepository
                .findByIdAndUserIdAndDeletedAtIsNull(categoryId, userId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        String name = TaskRules.trimRequired(request.getName(), "Category name is required");

        categoryRepository
                .findByUserIdAndNameIgnoreCaseAndDeletedAtIsNull(userId, name)
                .filter(existing -> !existing.getId().equals(categoryId))
                .ifPresent(existing -> {
                    throw new RuntimeException("Category name already exists");
                });

        category.setName(name);
        category.setColor(TaskRules.trimToNull(request.getColor()));

        category.setVersion(category.getVersion() == null ? 1 : category.getVersion() + 1);

        Category saved = categoryRepository.save(category);

        return CategoryResponse.from(saved);
    }

    public void deleteCategory(UUID userId, UUID categoryId) {
        Category category = categoryRepository
                .findByIdAndUserIdAndDeletedAtIsNull(categoryId, userId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        category.setDeletedAt(LocalDateTime.now());

        category.setVersion(category.getVersion() == null ? 1 : category.getVersion() + 1);

        categoryRepository.save(category);
    }
}
