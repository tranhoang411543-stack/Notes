package com.fini.todoapp.notification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.fini.todoapp.MainActivity
import com.fini.todoapp.R
import com.fini.todoapp.data.model.TaskResponse
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object TaskNotificationScheduler {

    const val CHANNEL_ID = "task_reminders"
    const val EXTRA_TASK_ID = "task_id"
    const val EXTRA_TITLE = "title"
    const val EXTRA_NOTE = "note"
    const val EXTRA_REPEAT_TYPE = "repeat_type"
    const val EXTRA_REPEAT_DAYS = "repeat_days"
    const val EXTRA_NOTIFY_AT = "notify_at"
    const val EXTRA_NOTIFY_TIME = "notify_time"
    const val EXTRA_AUTO_TRASH = "auto_trash"

    private const val PREF_NAME = "note_scheduled_reminders"
    private const val KEY_TASK_IDS = "task_ids"

    fun scheduleAll(context: Context, tasks: List<TaskResponse>) {
        ensureChannel(context)

        val appContext = context.applicationContext
        val preferences = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        preferences.getStringSet(KEY_TASK_IDS, emptySet()).orEmpty().forEach { taskId ->
            cancel(appContext, taskId)
        }

        val scheduledIds = tasks.mapNotNull { task ->
            if (schedule(appContext, task)) task.id else null
        }.toSet()

        preferences.edit().putStringSet(KEY_TASK_IDS, scheduledIds).apply()
    }

    fun schedule(context: Context, task: TaskResponse): Boolean {
        val triggerAtMillis = nextTriggerAtMillis(
            notifyAt = task.notifyAt ?: task.dueAt,
            notifyTime = task.notifyTime,
            repeatType = task.repeatType,
            repeatDays = task.repeatDays,
            nowMillis = System.currentTimeMillis()
        ) ?: return false

        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            putExtra(EXTRA_TASK_ID, task.id)
            putExtra(EXTRA_TITLE, task.title)
            putExtra(EXTRA_NOTE, task.note.orEmpty())
            putExtra(EXTRA_REPEAT_TYPE, task.repeatType)
            putExtra(EXTRA_REPEAT_DAYS, task.repeatDays.orEmpty())
            putExtra(EXTRA_NOTIFY_AT, task.notifyAt ?: task.dueAt.orEmpty())
            putExtra(EXTRA_NOTIFY_TIME, task.notifyTime.orEmpty())
            putExtra(EXTRA_AUTO_TRASH, task.autoTrashAfterNotification)
        }

        scheduleIntent(context, task.id, intent, triggerAtMillis)
        return true
    }

    fun scheduleNextFromIntent(context: Context, source: Intent) {
        val taskId = source.getStringExtra(EXTRA_TASK_ID) ?: return
        val repeatType = source.getStringExtra(EXTRA_REPEAT_TYPE).orEmpty().uppercase()
        if (repeatType == "NONE") {
            return
        }

        val next = nextTriggerAtMillis(
            notifyAt = null,
            notifyTime = source.getStringExtra(EXTRA_NOTIFY_TIME),
            repeatType = repeatType,
            repeatDays = source.getStringExtra(EXTRA_REPEAT_DAYS),
            nowMillis = System.currentTimeMillis() + 1000L
        ) ?: return

        scheduleIntent(context, taskId, source, next)
    }

    fun cancel(context: Context, taskId: String) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.stableRequestCode(),
            Intent(context, TaskReminderReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun showNotification(context: Context, taskId: String, title: String, note: String) {
        ensureChannel(context)
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("task_id", taskId)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            taskId.stableRequestCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val displayTitle = title.ifBlank { "Note" }
        val rawNoteText = note.ifBlank { "Đến giờ xem ghi chú." }
        val rawBigText = if (note.isBlank()) title.ifBlank { "Đến giờ xem ghi chú." } else note

        fun truncateText(text: String): String {
            val maxLines = 3
            val lines = text.lines()
            return if (lines.size > maxLines) {
                lines.take(maxLines).joinToString("\n") + "\n..."
            } else if (text.length > 150) {
                text.take(150) + "..."
            } else {
                text
            }
        }

        val truncatedContentText = truncateText(rawNoteText)
        val truncatedBigText = truncateText(rawBigText)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFFFFFFFF.toInt())
            .setContentTitle(displayTitle)
            .setContentText(truncatedContentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(truncatedBigText))
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(context).notify(taskId.stableRequestCode(), notification)
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Ghi chú",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Thông báo ghi chú theo giờ đã chọn"
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun scheduleIntent(context: Context, taskId: String, intent: Intent, triggerAtMillis: Long) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.stableRequestCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun nextTriggerAtMillis(
        notifyAt: String?,
        notifyTime: String?,
        repeatType: String,
        repeatDays: String?,
        nowMillis: Long
    ): Long? {
        val normalizedRepeat = repeatType.uppercase()
        if (normalizedRepeat == "NONE") {
            val millis = parseApiDateTime(notifyAt)?.timeInMillis ?: return null
            return millis.takeIf { it >= nowMillis }
        }

        val (hour, minute) = parseNotifyTime(notifyTime) ?: return null
        val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val selectedDays = parseRepeatDays(repeatDays)

        for (offset in 0..13) {
            val candidate = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                add(Calendar.DAY_OF_YEAR, offset)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (normalizedRepeat == "WEEKLY" && !selectedDays.contains(candidate.get(Calendar.DAY_OF_WEEK))) {
                continue
            }

            if (candidate.timeInMillis >= now.timeInMillis) {
                return candidate.timeInMillis
            }
        }

        return null
    }

    private fun parseApiDateTime(value: String?): Calendar? {
        if (value.isNullOrBlank()) return null
        return runCatching {
            Calendar.getInstance().apply {
                time = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(value)!!
            }
        }.getOrNull()
    }

    private fun parseNotifyTime(value: String?): Pair<Int, Int>? {
        val parts = value.orEmpty().split(":")
        if (parts.size < 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        return hour to minute
    }

    private fun parseRepeatDays(value: String?): Set<Int> {
        val days = value.orEmpty()
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
        return days.ifEmpty { setOf(Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) }
    }

    private fun String.stableRequestCode(): Int = hashCode()
}
