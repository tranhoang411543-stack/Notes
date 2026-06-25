package com.fini.todoapp.data.model

data class SyncCategoryItem(
    val id: String,
    val name: String,
    val color: String?,
    val deleted: Boolean,
    val version: Int,
    val createdAt: String?,
    val updatedAt: String?
)

data class SyncTaskItem(
    val id: String,
    val categoryId: String?,
    val title: String,
    val note: String?,
    val completed: Boolean,
    val priority: Boolean,
    val dueAt: String?,
    val reminderEnabled: Boolean,
    val notifyAt: String?,
    val notifyTime: String?,
    val autoTrashAfterNotification: Boolean,
    val repeatType: String,
    val repeatDays: String?,
    val hasLocation: Boolean,
    val latitude: Double?,
    val longitude: Double?,
    val locationName: String?,
    val address: String?,
    val deleted: Boolean,
    val trashedAt: String?,
    val purgeAfter: String?,
    val version: Int,
    val createdAt: String?,
    val updatedAt: String?
)

data class SyncRequest(
    val deviceId: String?,
    val deviceType: String,
    val deviceName: String,
    val fcmToken: String?,
    val lastSyncAt: String?,
    val changedCategories: List<SyncCategoryItem>,
    val changedTasks: List<SyncTaskItem>
)

data class SyncResponse(
    val deviceId: String,
    val serverTime: String,
    val categories: List<SyncCategoryItem>,
    val tasks: List<SyncTaskItem>
)
