package com.fini.todoapp.ui.home

import android.app.Application
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.fini.todoapp.AppSessionPolicy
import com.fini.todoapp.data.AuthTokenStore
import com.fini.todoapp.data.ErrorUtils
import com.fini.todoapp.data.local.FiniDatabase
import com.fini.todoapp.data.local.LocalCategory
import com.fini.todoapp.data.local.LocalTask
import com.fini.todoapp.data.model.CategoryResponse
import com.fini.todoapp.data.model.TaskRequest
import com.fini.todoapp.data.model.TaskResponse
import com.fini.todoapp.notification.SyncWorker
import com.fini.todoapp.notification.TaskNotificationScheduler
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID
data class HomeUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val categories: List<CategoryResponse> = emptyList(),
    val allTasks: List<TaskResponse> = emptyList(),
    val tasks: List<TaskResponse> = emptyList(),
    val dateFilter: String = "ALL",
    val selectedCategoryId: String? = null,
    val keyword: String = "",
    val scrollToTop: Boolean = false
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    var state by mutableStateOf(HomeUiState())
        private set

    init {
        // Observe sync work completion flow to trigger reload of local DB items
        var lastState: WorkInfo.State? = null
        val workManager = WorkManager.getInstance(application)
        viewModelScope.launch {
            workManager.getWorkInfosForUniqueWorkFlow("db_sync").collect { workInfos ->
                val workInfo = workInfos?.firstOrNull()
                if (workInfo != null) {
                    val currentState = workInfo.state
                    if (lastState != null && lastState != currentState) {
                        if (currentState == WorkInfo.State.SUCCEEDED) {
                            android.widget.Toast.makeText(
                                getApplication(),
                                "Đồng bộ thành công",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        } else if (currentState == WorkInfo.State.FAILED) {
                            android.widget.Toast.makeText(
                                getApplication(),
                                "Đồng bộ thất bại",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    lastState = currentState
                    if (currentState == WorkInfo.State.SUCCEEDED || currentState == WorkInfo.State.FAILED) {
                        Log.d("HomeViewModel", "Sync work finished with state: $currentState. Reloading local DB.")
                        loadLocalDataOnly()
                    }
                }
            }
        }
    }

    private fun loadLocalDataOnly() {
        viewModelScope.launch {
            try {
                val db = FiniDatabase.getDatabase(getApplication())
                val categories = db.categoryDao().getAllActive().map { it.toCategoryResponse() }
                val tasks = if (state.dateFilter == "TRASH") {
                    db.taskDao().getAll().filter { it.trashedAt != null && !it.deleted }.map { it.toTaskResponse() }
                } else {
                    db.taskDao().getAllActive().map { it.toTaskResponse() }
                }

                state = state.copy(
                    categories = categories,
                    allTasks = tasks,
                    tasks = tasks,
                    error = null
                ).withFilteredTasks()
            } catch (e: Exception) {
                state = state.copy(error = ErrorUtils.getMessage(e))
            }
        }
    }

    fun triggerSync() {
        if (!AuthTokenStore.hasSession()) {
            return
        }

        val workManager = WorkManager.getInstance(getApplication())
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()
        workManager.enqueueUniqueWork("db_sync", ExistingWorkPolicy.REPLACE, syncRequest)
    }

    private fun triggerSyncAfterLocalChange() {
        if (AppSessionPolicy.shouldSyncAfterLocalChange(AuthTokenStore.hasSession())) {
            triggerSync()
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getApplication<Application>().getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun loadAll(syncWithServer: Boolean = false) {
        loadCategories()
        loadTasks()
        if (!syncWithServer) {
            return
        }

        val hasNetwork = isNetworkAvailable()
        if (!hasNetwork) {
            android.widget.Toast.makeText(
                getApplication(),
                "Không có kết nối Internet",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        } else if (AppSessionPolicy.shouldRunSync(AuthTokenStore.hasSession(), hasNetwork)) {
            triggerSync()
        }
    }

    fun loadCategories() {
        viewModelScope.launch {
            try {
                val db = FiniDatabase.getDatabase(getApplication())
                val categories = db.categoryDao().getAllActive().map { it.toCategoryResponse() }
                state = state.copy(categories = categories, error = null)
            } catch (e: Exception) {
                state = state.copy(error = ErrorUtils.getMessage(e))
            }
        }
    }
    fun loadTasks() {
        if (state.dateFilter == "TRASH") {
            openTrash()
            return
        }
        state = state.copy(loading = true, error = null)

        viewModelScope.launch {
            try {
                val db = FiniDatabase.getDatabase(getApplication())
                val tasks = db.taskDao().getAllActive().map { it.toTaskResponse() }

                state = state.copy(
                    loading = false,
                    allTasks = tasks,
                    tasks = tasks,
                    error = null
                ).withFilteredTasks()
            } catch (e: Exception) {
                state = state.copy(
                    loading = false,
                    error = ErrorUtils.getMessage(e)
                )
            }
        }
    }

    fun setDateFilter(filter: String) {
        if (state.dateFilter == "TRASH") {
            state = state.copy(dateFilter = filter, scrollToTop = true)
            loadTasks()
        } else {
            state = state.copy(dateFilter = filter, scrollToTop = true).withFilteredTasks()
        }
    }

    fun setCategoryFilter(categoryId: String?) {
        state = state.copy(selectedCategoryId = categoryId, scrollToTop = true).withFilteredTasks()
    }

    fun setKeyword(keyword: String) {
        state = state.copy(keyword = keyword, scrollToTop = true).withFilteredTasks()
    }

    fun createCategory(name: String, color: String?) {
        viewModelScope.launch {
            try {
                val db = FiniDatabase.getDatabase(getApplication())
                val nowStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Calendar.getInstance().time)
                val newCat = LocalCategory(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    color = color,
                    createdAt = nowStr,
                    updatedAt = null,
                    deleted = false,
                    syncStatus = "PENDING_CREATE",
                    version = 0
                )
                db.categoryDao().insert(newCat)
                loadCategories()
                triggerSyncAfterLocalChange()
            } catch (e: Exception) {
                state = state.copy(error = ErrorUtils.getMessage(e))
            }
        }
    }

    fun createTask(request: TaskRequest) {
        viewModelScope.launch {
            try {
                val db = FiniDatabase.getDatabase(getApplication())
                val nowStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Calendar.getInstance().time)
                val newTask = LocalTask(
                    id = UUID.randomUUID().toString(),
                    categoryId = request.categoryId,
                    title = request.title,
                    note = request.note,
                    completed = false,
                    priority = request.priority,
                    notifyAt = request.notifyAt,
                    notifyTime = request.notifyTime,
                    autoTrashAfterNotification = request.autoTrashAfterNotification,
                    dueAt = request.dueAt,
                    reminderEnabled = request.reminderEnabled,
                    repeatType = request.repeatType,
                    repeatDays = request.repeatDays,
                    hasLocation = request.hasLocation,
                    latitude = request.latitude,
                    longitude = request.longitude,
                    locationName = request.locationName,
                    address = request.address,
                    createdAt = nowStr,
                    updatedAt = null,
                    trashedAt = null,
                    purgeAfter = null,
                    deleted = false,
                    syncStatus = "PENDING_CREATE",
                    version = 0
                )
                db.taskDao().insert(newTask)
                TaskNotificationScheduler.schedule(getApplication(), newTask.toTaskResponse())
                
                val tasks = db.taskDao().getAllActive().map { it.toTaskResponse() }
                state = state.copy(
                    loading = false,
                    allTasks = tasks,
                    tasks = tasks,
                    error = null,
                    scrollToTop = true
                ).withFilteredTasks()
                triggerSyncAfterLocalChange()
            } catch (e: Exception) {
                state = state.copy(error = ErrorUtils.getMessage(e))
            }
        }
    }

    fun updateTask(taskId: String, request: TaskRequest) {
        viewModelScope.launch {
            try {
                val db = FiniDatabase.getDatabase(getApplication())
                val existing = db.taskDao().getById(taskId)
                if (existing != null) {
                    val nowStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Calendar.getInstance().time)
                    val updated = existing.copy(
                        categoryId = request.categoryId,
                        title = request.title,
                        note = request.note,
                        priority = request.priority,
                        notifyAt = request.notifyAt,
                        notifyTime = request.notifyTime,
                        autoTrashAfterNotification = request.autoTrashAfterNotification,
                        dueAt = request.dueAt,
                        reminderEnabled = request.reminderEnabled,
                        repeatType = request.repeatType,
                        repeatDays = request.repeatDays,
                        hasLocation = request.hasLocation,
                        latitude = request.latitude,
                        longitude = request.longitude,
                        locationName = request.locationName,
                        address = request.address,
                        updatedAt = nowStr,
                        syncStatus = if (existing.syncStatus == "PENDING_CREATE") "PENDING_CREATE" else "PENDING_UPDATE"
                    )
                    db.taskDao().update(updated)
                    TaskNotificationScheduler.schedule(getApplication(), updated.toTaskResponse())
                }
                loadTasks()
                triggerSyncAfterLocalChange()
            } catch (e: Exception) {
                state = state.copy(error = ErrorUtils.getMessage(e))
            }
        }
    }

    fun togglePriority(task: TaskResponse) {
        viewModelScope.launch {
            try {
                val db = FiniDatabase.getDatabase(getApplication())
                val existing = db.taskDao().getById(task.id)
                if (existing != null) {
                    val updated = existing.copy(
                        priority = !existing.priority,
                        syncStatus = if (existing.syncStatus == "PENDING_CREATE") "PENDING_CREATE" else "PENDING_UPDATE"
                    )
                    db.taskDao().update(updated)
                }
                loadTasks()
                triggerSyncAfterLocalChange()
            } catch (e: Exception) {
                state = state.copy(error = ErrorUtils.getMessage(e))
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            try {
                val db = FiniDatabase.getDatabase(getApplication())
                val existing = db.taskDao().getById(taskId)
                if (existing != null) {
                    val nowStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Calendar.getInstance().time)
                    val updated = existing.copy(
                        trashedAt = nowStr,
                        syncStatus = if (existing.syncStatus == "PENDING_CREATE") "PENDING_CREATE" else "PENDING_UPDATE"
                    )
                    db.taskDao().update(updated)
                }
                loadTasks()
                triggerSyncAfterLocalChange()
            } catch (e: Exception) {
                state = state.copy(error = ErrorUtils.getMessage(e))
            }
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            try {
                val db = FiniDatabase.getDatabase(getApplication())
                val existing = db.categoryDao().getById(categoryId)
                if (existing != null) {
                    if (existing.syncStatus == "PENDING_CREATE") {
                        db.categoryDao().delete(existing)
                    } else {
                        db.categoryDao().update(existing.copy(deleted = true, syncStatus = "PENDING_DELETE"))
                    }
                }
                state = state.copy(
                    selectedCategoryId = if (state.selectedCategoryId == categoryId) {
                        null
                    } else {
                        state.selectedCategoryId
                    }
                )
                loadCategories()
                if (state.dateFilter == "TRASH") {
                    openTrash()
                } else {
                    loadTasks()
                }
                triggerSyncAfterLocalChange()
            } catch (e: Exception) {
                state = state.copy(error = ErrorUtils.getMessage(e))
            }
        }
    }

    fun openTrash() {
        state = state.copy(loading = true, error = null, dateFilter = "TRASH", scrollToTop = true)

        viewModelScope.launch {
            try {
                val db = FiniDatabase.getDatabase(getApplication())
                val trash = db.taskDao().getAll().filter { it.trashedAt != null && !it.deleted }.map { it.toTaskResponse() }
                state = state.copy(
                    loading = false,
                    allTasks = trash,
                    tasks = trash,
                    error = null,
                    scrollToTop = true
                ).withFilteredTasks()
            } catch (e: Exception) {
                state = state.copy(
                    loading = false,
                    error = ErrorUtils.getMessage(e)
                )
            }
        }
    }

    fun clearTrash() {
        state = state.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val db = FiniDatabase.getDatabase(getApplication())
                val trashedTasks = db.taskDao().getAll().filter { it.trashedAt != null && !it.deleted }
                for (task in trashedTasks) {
                    if (task.syncStatus == "PENDING_CREATE") {
                        db.taskDao().delete(task)
                    } else {
                        db.taskDao().update(task.copy(deleted = true, syncStatus = "PENDING_DELETE"))
                    }
                }
                openTrash()
                triggerSyncAfterLocalChange()
            } catch (e: Exception) {
                state = state.copy(loading = false, error = ErrorUtils.getMessage(e))
            }
        }
    }

    fun restoreTask(taskId: String) {
        viewModelScope.launch {
            try {
                val db = FiniDatabase.getDatabase(getApplication())
                val existing = db.taskDao().getById(taskId)
                if (existing != null) {
                    val updated = existing.copy(
                        trashedAt = null,
                        syncStatus = if (existing.syncStatus == "PENDING_CREATE") "PENDING_CREATE" else "PENDING_UPDATE"
                    )
                    db.taskDao().update(updated)
                }
                openTrash()
                triggerSyncAfterLocalChange()
            } catch (e: Exception) {
                state = state.copy(error = ErrorUtils.getMessage(e))
            }
        }
    }

    fun clearError() {
        state = state.copy(error = null)
    }

    fun resetScrollToTop() {
        state = state.copy(scrollToTop = false)
    }

    fun resetFilters() {
        state = state.copy(
            dateFilter = "ALL",
            selectedCategoryId = null,
            keyword = ""
        ).withFilteredTasks()
    }

    fun resetSession() {
        state = HomeUiState()
    }

    fun clearLocalData() {
        viewModelScope.launch {
            try {
                val db = FiniDatabase.getDatabase(getApplication())
                db.categoryDao().deleteAll()
                db.taskDao().deleteAll()
                val sharedPrefs = getApplication<Application>().getSharedPreferences("fini_sync_prefs", android.content.Context.MODE_PRIVATE)
                sharedPrefs.edit().clear().apply()

                // Cancel all scheduled alarms and clear the reminder preferences
                val appContext = getApplication<Application>()
                val reminderPrefs = appContext.getSharedPreferences("note_scheduled_reminders", android.content.Context.MODE_PRIVATE)
                reminderPrefs.getStringSet("task_ids", emptySet()).orEmpty().forEach { taskId ->
                    TaskNotificationScheduler.cancel(appContext, taskId)
                }
                reminderPrefs.edit().clear().apply()

                Log.d("HomeViewModel", "Database, sync preferences, and scheduled alarms cleared.")
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error clearing database", e)
            }
        }
    }

    private fun HomeUiState.withFilteredTasks(): HomeUiState {
        return copy(tasks = filterTasks(this))
    }

    private fun filterTasks(snapshot: HomeUiState): List<TaskResponse> {
        val normalizedKeyword = snapshot.keyword.trim().lowercase()
        val todayStart = startOfToday()
        val tomorrowStart = calendarClone(todayStart).apply { add(Calendar.DAY_OF_YEAR, 1) }
        val nextWeekStart = endOfThisWeekExclusive()

        return snapshot.allTasks.filter { task ->
            val matchesCategory = snapshot.selectedCategoryId == null ||
                    task.categoryId == snapshot.selectedCategoryId
            val matchesKeyword = normalizedKeyword.isBlank() ||
                    task.title.lowercase().contains(normalizedKeyword) ||
                    task.note.orEmpty().lowercase().contains(normalizedKeyword)
            val nextNotification = nextNotificationAt(task, todayStart)
            val matchesDate = when (snapshot.dateFilter) {
                "TODAY" -> nextNotification != null &&
                        !nextNotification.before(todayStart) &&
                        nextNotification.before(tomorrowStart)
                "THIS_WEEK" -> nextNotification != null &&
                        !nextNotification.before(todayStart) &&
                        nextNotification.before(nextWeekStart)
                "PRIORITY" -> task.priority
                else -> true
            }

            matchesCategory && matchesKeyword && matchesDate
        }.let { tasks ->
            when (snapshot.dateFilter) {
                "TODAY", "THIS_WEEK" -> tasks.sortedWith(
                    compareBy<TaskResponse> {
                        nextNotificationAt(it, todayStart)?.timeInMillis ?: Long.MAX_VALUE
                    }.thenByDescending {
                        calendarFromApiValue(it.createdAt)?.timeInMillis ?: Long.MIN_VALUE
                    }.thenBy { it.title.lowercase() }
                )
                "TRASH" -> tasks.sortedWith(
                    compareByDescending<TaskResponse> {
                        calendarFromApiValue(it.trashedAt)?.timeInMillis ?: Long.MIN_VALUE
                    }.thenBy { it.title.lowercase() }
                )
                else -> tasks.sortedWith(
                    compareByDescending<TaskResponse> {
                        calendarFromApiValue(it.createdAt)?.timeInMillis ?: Long.MIN_VALUE
                    }.thenBy { it.title.lowercase() }
                )
            }
        }
    }

    private fun parseRepeatDays(repeatDays: String?): Set<Int> {
        return repeatDays.orEmpty()
            .split(",", " ")
            .mapNotNull { token ->
                when (token.trim().uppercase()) {
                    "1", "MON", "MONDAY" -> Calendar.MONDAY
                    "2", "TUE", "TUESDAY" -> Calendar.TUESDAY
                    "3", "WED", "WEDNESDAY" -> Calendar.WEDNESDAY
                    "4", "THU", "THURSDAY" -> Calendar.THURSDAY
                    "5", "FRI", "FRIDAY" -> Calendar.FRIDAY
                    "6", "SAT", "SATURDAY" -> Calendar.SATURDAY
                    "7", "SUN", "SUNDAY" -> Calendar.SUNDAY
                    else -> null
                }
            }
            .toSet()
    }

    private fun calendarFromApiValue(value: String?): Calendar? {
        if (value.isNullOrBlank()) {
            return null
        }
        return runCatching {
            Calendar.getInstance().apply {
                time = apiFormat().parse(value)!!
            }
        }.getOrNull()
    }

    private fun nextNotificationAt(task: TaskResponse, fromInclusive: Calendar): Calendar? {
        val repeatType = task.repeatType.uppercase()
        if (repeatType == "NONE") {
            return calendarFromApiValue(task.notifyAt ?: task.dueAt)
        }

        val (hour, minute) = parseNotifyTime(task.notifyTime)
            ?: parseTimeFromApiDate(task.dueAt)
            ?: return null
        val selectedDays = parseRepeatDays(task.repeatDays)
        val cursor = calendarClone(fromInclusive)

        repeat(14) {
            val candidate = calendarClone(cursor).apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val matchesDay = repeatType == "DAILY" ||
                    selectedDays.contains(candidate.get(Calendar.DAY_OF_WEEK))
            if (matchesDay && !candidate.before(fromInclusive)) {
                return candidate
            }

            cursor.add(Calendar.DAY_OF_YEAR, 1)
        }

        return null
    }

    private fun parseNotifyTime(value: String?): Pair<Int, Int>? {
        val parts = value.orEmpty().split(":")
        if (parts.size < 2) {
            return null
        }
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        return hour to minute
    }

    private fun parseTimeFromApiDate(value: String?): Pair<Int, Int>? {
        val calendar = calendarFromApiValue(value) ?: return null
        return calendar.get(Calendar.HOUR_OF_DAY) to calendar.get(Calendar.MINUTE)
    }

    private fun startOfToday(): Calendar {
        return todayAtMidnight()
    }


    private fun endOfThisWeekExclusive(): Calendar {
        return todayAtMidnight().apply {
            val daysUntilNextMonday = (Calendar.MONDAY - get(Calendar.DAY_OF_WEEK) + 7) % 7
            add(Calendar.DAY_OF_YEAR, if (daysUntilNextMonday == 0) 7 else daysUntilNextMonday)
        }
    }

    private fun todayAtMidnight(): Calendar {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private fun calendarClone(calendar: Calendar): Calendar {
        return calendar.clone() as Calendar
    }

    private fun apiFormat(): SimpleDateFormat {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    }

    private fun LocalCategory.toCategoryResponse() = CategoryResponse(
        id = id,
        name = name,
        color = color,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun LocalTask.toTaskResponse() = TaskResponse(
        id = id,
        categoryId = categoryId,
        title = title,
        note = note,
        completed = completed,
        priority = priority,
        notifyAt = notifyAt,
        notifyTime = notifyTime,
        autoTrashAfterNotification = autoTrashAfterNotification,
        dueAt = dueAt,
        reminderEnabled = reminderEnabled,
        repeatType = repeatType,
        repeatDays = repeatDays,
        hasLocation = hasLocation,
        latitude = latitude,
        longitude = longitude,
        locationName = locationName,
        address = address,
        createdAt = createdAt,
        updatedAt = updatedAt,
        trashedAt = trashedAt,
        purgeAfter = purgeAfter
    )
}
