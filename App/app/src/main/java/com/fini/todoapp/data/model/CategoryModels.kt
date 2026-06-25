package com.fini.todoapp.data.model

data class CategoryRequest(
    val name: String,
    val color: String?
)

data class CategoryResponse(
    val id: String,
    val name: String,
    val color: String?,
    val createdAt: String?,
    val updatedAt: String?
)