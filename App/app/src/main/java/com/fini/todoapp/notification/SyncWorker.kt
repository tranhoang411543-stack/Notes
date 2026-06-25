package com.fini.todoapp.notification

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fini.todoapp.data.AuthTokenStore
import com.fini.todoapp.data.api.ApiClient
import com.fini.todoapp.data.local.FiniDatabase
import com.fini.todoapp.data.local.LocalCategory
import com.fini.todoapp.data.local.LocalTask
import com.fini.todoapp.data.model.SyncCategoryItem
import com.fini.todoapp.data.model.SyncRequest
import com.fini.todoapp.data.model.SyncTaskItem
import java.util.UUID

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "SyncWorker"
        private const val PREFS_NAME = "fini_sync_prefs"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_LAST_SYNC_AT = "last_sync_at"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Sync process started.")

        // Check auth token
        val token = AuthTokenStore.accessToken
        if (token.isNullOrBlank()) {
            Log.d(TAG, "No authentication token found. Skipping sync.")
            return Result.success()
        }

        val db = FiniDatabase.getDatabase(applicationContext)
        val sharedPrefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Read/Generate device ID
        var deviceId = sharedPrefs.getString(KEY_DEVICE_ID, null)
        if (deviceId.isNullOrBlank()) {
            deviceId = UUID.randomUUID().toString()
            sharedPrefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        }

        // Determine device info
        val isAutomotive = applicationContext.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE) ||
                (applicationContext.getSystemService(Context.UI_MODE_SERVICE) as? android.app.UiModeManager)?.currentModeType == android.content.res.Configuration.UI_MODE_TYPE_CAR ||
                android.os.Build.DEVICE.contains("car", ignoreCase = true) ||
                android.os.Build.MODEL.contains("car", ignoreCase = true) ||
                android.os.Build.FINGERPRINT.contains("car", ignoreCase = true)
        val deviceType = if (isAutomotive) "CAR" else "PHONE"
        val deviceName = android.os.Build.MODEL

        val lastSyncAt = sharedPrefs.getString(KEY_LAST_SYNC_AT, null)

        try {
            // Fetch unsynced local categories & tasks
            val unsyncedCategories = db.categoryDao().getUnsynced()
            val unsyncedTasks = db.taskDao().getUnsynced()

            Log.d(TAG, "Found ${unsyncedCategories.size} unsynced categories and ${unsyncedTasks.size} unsynced tasks.")

            val categoryItems = unsyncedCategories.map {
                SyncCategoryItem(
                    id = it.id,
                    name = it.name,
                    color = it.color,
                    deleted = it.deleted || it.syncStatus == "PENDING_DELETE",
                    version = it.version,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            }

            val taskItems = unsyncedTasks.map {
                SyncTaskItem(
                    id = it.id,
                    categoryId = it.categoryId,
                    title = it.title,
                    note = it.note,
                    completed = it.completed,
                    priority = it.priority,
                    dueAt = it.dueAt,
                    reminderEnabled = it.reminderEnabled,
                    notifyAt = it.notifyAt,
                    notifyTime = it.notifyTime,
                    autoTrashAfterNotification = it.autoTrashAfterNotification,
                    repeatType = it.repeatType,
                    repeatDays = it.repeatDays,
                    hasLocation = it.hasLocation,
                    latitude = it.latitude,
                    longitude = it.longitude,
                    locationName = it.locationName,
                    address = it.address,
                    deleted = it.deleted || it.syncStatus == "PENDING_DELETE",
                    trashedAt = it.trashedAt,
                    purgeAfter = it.purgeAfter,
                    version = it.version,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            }

            val syncRequest = SyncRequest(
                deviceId = deviceId,
                deviceType = deviceType,
                deviceName = deviceName,
                fcmToken = null,
                lastSyncAt = lastSyncAt,
                changedCategories = categoryItems,
                changedTasks = taskItems
            )

            val response = ApiClient.syncApi.sync(syncRequest)
            if (!response.isSuccessful) {
                Log.e(TAG, "Sync request failed: ${response.code()} - ${response.errorBody()?.string()}")
                if (response.code() == 401) {
                    Log.d(TAG, "Unauthorized response (401). Clearing token.")
                    AuthTokenStore.clear()
                    return Result.success()
                }
                return Result.retry()
            }

            val syncResponse = response.body()
            if (syncResponse == null) {
                Log.e(TAG, "Received empty sync response.")
                return Result.retry()
            }

            Log.d(TAG, "Sync success. Server returned ${syncResponse.categories.size} categories and ${syncResponse.tasks.size} tasks.")

            // 1. Process categories from server
            for (srvCat in syncResponse.categories) {
                if (srvCat.deleted) {
                    db.categoryDao().deleteById(srvCat.id)
                } else {
                    val existing = db.categoryDao().getById(srvCat.id)
                    val localCat = LocalCategory(
                        id = srvCat.id,
                        name = srvCat.name,
                        color = srvCat.color,
                        deleted = false,
                        syncStatus = "SYNCED",
                        version = srvCat.version,
                        createdAt = srvCat.createdAt ?: existing?.createdAt,
                        updatedAt = srvCat.updatedAt ?: existing?.updatedAt
                    )
                    db.categoryDao().insert(localCat)
                }
            }

            // 2. Process tasks from server
            for (srvTask in syncResponse.tasks) {
                if (srvTask.deleted) {
                    db.taskDao().deleteById(srvTask.id)
                } else {
                    val existing = db.taskDao().getById(srvTask.id)
                    val localTask = LocalTask(
                        id = srvTask.id,
                        categoryId = srvTask.categoryId,
                        title = srvTask.title,
                        note = srvTask.note,
                        completed = srvTask.completed,
                        priority = srvTask.priority,
                        notifyAt = srvTask.notifyAt,
                        notifyTime = srvTask.notifyTime,
                        autoTrashAfterNotification = srvTask.autoTrashAfterNotification,
                        dueAt = srvTask.dueAt,
                        reminderEnabled = srvTask.reminderEnabled,
                        repeatType = srvTask.repeatType,
                        repeatDays = srvTask.repeatDays,
                        hasLocation = srvTask.hasLocation,
                        latitude = srvTask.latitude,
                        longitude = srvTask.longitude,
                        locationName = srvTask.locationName,
                        address = srvTask.address,
                        deleted = false,
                        syncStatus = "SYNCED",
                        version = srvTask.version,
                        createdAt = srvTask.createdAt ?: existing?.createdAt,
                        updatedAt = srvTask.updatedAt ?: existing?.updatedAt,
                        trashedAt = srvTask.trashedAt,
                        purgeAfter = srvTask.purgeAfter
                    )
                    db.taskDao().insert(localTask)
                }
            }

            // 3. Clear soft deleted entries that were pending delete and successfully sent
            unsyncedCategories.filter { it.deleted || it.syncStatus == "PENDING_DELETE" }.forEach {
                db.categoryDao().deleteById(it.id)
            }
            unsyncedTasks.filter { it.deleted || it.syncStatus == "PENDING_DELETE" }.forEach {
                db.taskDao().deleteById(it.id)
            }

            // 4. Update the sync status of local items that we successfully sent to server
            unsyncedCategories.filter { it.syncStatus != "PENDING_DELETE" && !it.deleted }.forEach {
                val current = db.categoryDao().getById(it.id)
                if (current != null && current.syncStatus != "SYNCED") {
                    db.categoryDao().update(current.copy(syncStatus = "SYNCED"))
                }
            }
            unsyncedTasks.filter { it.syncStatus != "PENDING_DELETE" && !it.deleted }.forEach {
                val current = db.taskDao().getById(it.id)
                if (current != null && current.syncStatus != "SYNCED") {
                    db.taskDao().update(current.copy(syncStatus = "SYNCED"))
                }
            }

            // 5. Update local shared preference flags for device id and last sync time
            sharedPrefs.edit()
                .putString(KEY_DEVICE_ID, syncResponse.deviceId)
                .putString(KEY_LAST_SYNC_AT, syncResponse.serverTime)
                .apply()

            // 6. Reschedule notifications for updated tasks
            val activeTasks = db.taskDao().getAllActive().map { localTask ->
                com.fini.todoapp.data.model.TaskResponse(
                    id = localTask.id,
                    categoryId = localTask.categoryId,
                    title = localTask.title,
                    note = localTask.note,
                    completed = localTask.completed,
                    priority = localTask.priority,
                    notifyAt = localTask.notifyAt,
                    notifyTime = localTask.notifyTime,
                    autoTrashAfterNotification = localTask.autoTrashAfterNotification,
                    dueAt = localTask.dueAt,
                    reminderEnabled = localTask.reminderEnabled,
                    repeatType = localTask.repeatType,
                    repeatDays = localTask.repeatDays,
                    hasLocation = localTask.hasLocation,
                    latitude = localTask.latitude,
                    longitude = localTask.longitude,
                    locationName = localTask.locationName,
                    address = localTask.address,
                    createdAt = localTask.createdAt,
                    updatedAt = localTask.updatedAt,
                    trashedAt = localTask.trashedAt,
                    purgeAfter = localTask.purgeAfter
                )
            }
            TaskNotificationScheduler.scheduleAll(applicationContext, activeTasks)

            Log.d(TAG, "Sync complete and saved. Server time updated to: ${syncResponse.serverTime}")
            return Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Sync error occurred", e)
            return Result.retry()
        }
    }
}
