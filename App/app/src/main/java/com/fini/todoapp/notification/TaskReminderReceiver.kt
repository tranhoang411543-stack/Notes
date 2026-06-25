package com.fini.todoapp.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.app.UiModeManager
import android.content.res.Configuration
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.fini.todoapp.data.AuthTokenStore
import com.fini.todoapp.data.api.ApiClient
import com.fini.todoapp.data.local.FiniDatabase
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

class TaskReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(TaskNotificationScheduler.EXTRA_TASK_ID) ?: return
        val title = intent.getStringExtra(TaskNotificationScheduler.EXTRA_TITLE).orEmpty()
        val note = intent.getStringExtra(TaskNotificationScheduler.EXTRA_NOTE).orEmpty()
        val autoTrash = intent.getBooleanExtra(TaskNotificationScheduler.EXTRA_AUTO_TRASH, false)
        Log.d("TaskReminderReceiver", "onReceive triggered. taskId=$taskId, title=$title, note=$note")
        val pendingResult = goAsync()

        TaskNotificationScheduler.showNotification(context, taskId, title, note)

        CoroutineScope(Dispatchers.Main).launch {
            val ttsJob = launch {
                try {
                    if (isAutomotive(context)) {
                        val speakText = if (note.isBlank()) {
                            "Thông báo: $title"
                        } else {
                            "Thông báo: $title. Nội dung: $note"
                        }
                        Log.d("TaskReminderReceiver", "speakText: $speakText")
                        speakTts(context, speakText)
                    } else {
                        Log.d("TaskReminderReceiver", "Not Automotive device")
                    }
                } catch (e: Exception) {
                    Log.e("TaskReminderReceiver", "Error in ttsJob", e)
                }
            }

            val ioJob = launch(Dispatchers.IO) {
                try {
                    val db = FiniDatabase.getDatabase(context.applicationContext)
                    val existing = db.taskDao().getById(taskId)
                    if (existing != null && autoTrash) {
                        val nowStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Calendar.getInstance().time)
                        val updated = existing.copy(
                            trashedAt = nowStr,
                            syncStatus = if (existing.syncStatus == "PENDING_CREATE") "PENDING_CREATE" else "PENDING_UPDATE"
                        )
                        db.taskDao().update(updated)
                        
                        // Trigger SyncWorker
                        val workManager = WorkManager.getInstance(context.applicationContext)
                        val constraints = Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()
                        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                            .setConstraints(constraints)
                            .build()
                        workManager.enqueueUniqueWork("db_sync", ExistingWorkPolicy.REPLACE, syncRequest)
                    }
                } catch (e: Exception) {
                    Log.e("TaskReminderReceiver", "Error local trashing task", e)
                }

                try {
                    AuthTokenStore.init(context.applicationContext)
                    if (autoTrash) {
                        ApiClient.taskApi.trashTask(taskId)
                    } else {
                        TaskNotificationScheduler.scheduleNextFromIntent(context.applicationContext, intent)
                    }
                } catch (_: Exception) {
                    if (!autoTrash) {
                        TaskNotificationScheduler.scheduleNextFromIntent(context.applicationContext, intent)
                    }
                }
            }

            ttsJob.join()
            ioJob.join()
            pendingResult.finish()
        }
    }

    private fun isAutomotive(context: Context): Boolean {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE) ||
                uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_CAR ||
                Build.DEVICE.contains("car", ignoreCase = true) ||
                Build.MODEL.contains("car", ignoreCase = true) ||
                Build.FINGERPRINT.contains("car", ignoreCase = true) ||
                (context.resources.configuration.screenWidthDp >= 800 && context.resources.configuration.screenWidthDp > context.resources.configuration.screenHeightDp)
    }

    private suspend fun speakTts(context: Context, text: String) = suspendCancellableCoroutine<Unit> { continuation ->
        Log.d("TaskReminderReceiver", "speakTts starting: $text")
        var tts: TextToSpeech? = null
        tts = TextToSpeech(context.applicationContext) { status ->
            Log.d("TaskReminderReceiver", "TextToSpeech init status=$status")
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale("vi", "VN"))
                Log.d("TaskReminderReceiver", "setLanguage Locale(\"vi\", \"VN\") result=$result")
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    val fallback = tts?.setLanguage(Locale.getDefault())
                    Log.d("TaskReminderReceiver", "Fallback to default locale result=$fallback")
                }

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        Log.d("TaskReminderReceiver", "TTS onStart")
                    }
                    override fun onDone(utteranceId: String?) {
                        Log.d("TaskReminderReceiver", "TTS onDone")
                        tts?.shutdown()
                        if (continuation.isActive) continuation.resume(Unit)
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        Log.e("TaskReminderReceiver", "TTS onError")
                        tts?.shutdown()
                        if (continuation.isActive) continuation.resume(Unit)
                    }
                    override fun onError(utteranceId: String?, errorCode: Int) {
                        Log.e("TaskReminderReceiver", "TTS onError errorCode=$errorCode")
                        tts?.shutdown()
                        if (continuation.isActive) continuation.resume(Unit)
                    }
                })

                val speakResult = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "reminder_tts")
                Log.d("TaskReminderReceiver", "speakResult=$speakResult")
                if (speakResult != TextToSpeech.SUCCESS) {
                    tts?.shutdown()
                    if (continuation.isActive) continuation.resume(Unit)
                }
            } else {
                tts?.shutdown()
                if (continuation.isActive) continuation.resume(Unit)
            }
        }

        continuation.invokeOnCancellation {
            tts?.stop()
            tts?.shutdown()
        }
    }
}
