package com.fini.todoapp.data.model

data class TaskRequest(
    val categoryId: String?,
    val title: String,
    val note: String?,
    val priority: Boolean,
    val notifyAt: String?,
    val notifyTime: String?,
    val autoTrashAfterNotification: Boolean,
    val dueAt: String?,
    val reminderEnabled: Boolean,
    val repeatType: String,
    val repeatDays: String?,
    val hasLocation: Boolean,
    val latitude: Double?,
    val longitude: Double?,
    val locationName: String?,
    val address: String?
)

data class TaskResponse(
    val id: String,
    val categoryId: String?,
    val title: String,
    val note: String?,
    val completed: Boolean,
    val priority: Boolean,
    val notifyAt: String?,
    val notifyTime: String?,
    val autoTrashAfterNotification: Boolean,
    val dueAt: String?,
    val reminderEnabled: Boolean,
    val repeatType: String,
    val repeatDays: String?,
    val hasLocation: Boolean,
    val latitude: Double?,
    val longitude: Double?,
    val locationName: String?,
    val address: String?,
    val createdAt: String?,
    val updatedAt: String?,
    val trashedAt: String?,
    val purgeAfter: String?
)
