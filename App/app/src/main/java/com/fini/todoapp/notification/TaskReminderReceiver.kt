package com.fini.todoapp.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.app.UiModeManager
import android.content.res.Configuration
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import com.fini.todoapp.data.local.FiniDatabase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

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
                    val automotive = isAutomotive(context)
                    if (automotive) {
                        var spoken = false
                        for (voiceUsage in ReminderVoiceAudioPolicy.usagesFor(automotive)) {
                            val result = withTimeoutOrNull(TTS_ATTEMPT_TIMEOUT_MS) {
                                speakTts(
                                    context = context,
                                    title = title,
                                    note = note,
                                    voiceUsage = voiceUsage
                                )
                            }
                            if (result == true) {
                                spoken = true
                                break
                            }
                            Log.w("TaskReminderReceiver", "TTS retry needed. usage=$voiceUsage, result=$result")
                        }
                        if (!spoken) {
                            Log.e("TaskReminderReceiver", "TTS did not complete with any audio usage")
                        }
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
                    }
                } catch (e: Exception) {
                    Log.e("TaskReminderReceiver", "Error local trashing task", e)
                }

                try {
                    if (!autoTrash) {
                        TaskNotificationScheduler.scheduleNextFromIntent(context.applicationContext, intent)
                    }
                } catch (e: Exception) {
                    Log.e("TaskReminderReceiver", "Error scheduling next reminder", e)
                }
            }

            ttsJob.join()
            ioJob.join()
            pendingResult.finish()
        }
    }

    private fun isAutomotive(context: Context): Boolean {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        val configuration = context.resources.configuration
        return ReminderDevicePolicy.isAutomotiveDevice(
            hasAutomotiveFeature = context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE),
            uiModeType = uiModeManager?.currentModeType ?: configuration.uiMode and Configuration.UI_MODE_TYPE_MASK,
            device = Build.DEVICE,
            model = Build.MODEL,
            fingerprint = Build.FINGERPRINT,
            screenWidthDp = configuration.screenWidthDp,
            screenHeightDp = configuration.screenHeightDp
        )
    }

    private suspend fun speakTts(
        context: Context,
        title: String,
        note: String,
        voiceUsage: ReminderVoiceUsage
    ) = suspendCancellableCoroutine<Boolean> { continuation ->
        Log.d("TaskReminderReceiver", "speakTts starting: title=$title usage=$voiceUsage")
        var tts: TextToSpeech? = null
        var audioFocusRequest: AudioFocusRequest? = null
        val audioManager = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val audioAttributes = audioAttributesFor(voiceUsage)

        fun doRelease() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
                audioManager?.abandonAudioFocusRequest(audioFocusRequest!!)
            }
            tts?.shutdown()
        }

        fun doSpeak(ttsEngine: TextToSpeech, speakText: String) {
            val speakResult = ttsEngine.speak(speakText, TextToSpeech.QUEUE_FLUSH, null, "reminder_tts")
            Log.d("TaskReminderReceiver", "speakResult=$speakResult usage=$voiceUsage")
            if (speakResult != TextToSpeech.SUCCESS) {
                doRelease()
                if (continuation.isActive) continuation.resume(false)
            }
        }

        tts = TextToSpeech(context.applicationContext) { status ->
            Log.d("TaskReminderReceiver", "TTS init status=$status usage=$voiceUsage")
            if (status != TextToSpeech.SUCCESS) {
                doRelease()
                if (continuation.isActive) continuation.resume(false)
                return@TextToSpeech
            }

            val ttsEngine = tts ?: run {
                doRelease()
                if (continuation.isActive) continuation.resume(false)
                return@TextToSpeech
            }

            ttsEngine.setAudioAttributes(audioAttributes)

            val activeLanguage = configureVoice(ttsEngine)
            val speakText = speechTextFor(activeLanguage, title, note)
            Log.d("TaskReminderReceiver", "speakText: $speakText language=$activeLanguage")

            ttsEngine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    Log.d("TaskReminderReceiver", "TTS onStart")
                }
                override fun onDone(utteranceId: String?) {
                    Log.d("TaskReminderReceiver", "TTS onDone")
                    doRelease()
                    if (continuation.isActive) continuation.resume(true)
                }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    Log.e("TaskReminderReceiver", "TTS onError")
                    doRelease()
                    if (continuation.isActive) continuation.resume(false)
                }
                override fun onError(utteranceId: String?, errorCode: Int) {
                    Log.e("TaskReminderReceiver", "TTS onError errorCode=$errorCode")
                    doRelease()
                    if (continuation.isActive) continuation.resume(false)
                }
            })

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioManager != null) {
                // Quan trọng: listener này xử lý cả trường hợp DELAYED trên Automotive OS.
                // CarAudioService thường không cấp focus ngay — nó phải thương lượng với
                // các audio zone khác. Khi focus cuối cùng được cấp (AUDIOFOCUS_GAIN),
                // ta mới gọi speak() lúc đó.
                val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
                    Log.d("TaskReminderReceiver", "TTS audio focus change=$focusChange usage=$voiceUsage")
                    if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                        // Focus bị delay nhưng giờ đã được cấp — speak ngay bây giờ
                        doSpeak(ttsEngine, speakText)
                    }
                }

                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(audioAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(focusChangeListener)
                    .build()

                when (val focusResult = audioManager.requestAudioFocus(audioFocusRequest!!)) {
                    AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                        Log.d("TaskReminderReceiver", "TTS audio focus GRANTED usage=$voiceUsage")
                        doSpeak(ttsEngine, speakText)
                    }
                    AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> {
                        // Rất phổ biến trên Automotive OS — CarAudioService đang thương lượng.
                        // doSpeak() sẽ được gọi từ focusChangeListener khi nhận AUDIOFOCUS_GAIN.
                        Log.d("TaskReminderReceiver", "TTS audio focus DELAYED usage=$voiceUsage - waiting for grant")
                    }
                    else -> {
                        Log.w("TaskReminderReceiver", "TTS audio focus FAILED result=$focusResult usage=$voiceUsage")
                        doRelease()
                        if (continuation.isActive) continuation.resume(false)
                    }
                }
            } else {
                // Pre-API 26: không có AudioFocusRequest API, speak trực tiếp
                doSpeak(ttsEngine, speakText)
            }
        }

        continuation.invokeOnCancellation {
            tts?.stop()
            doRelease()
        }
    }

    private fun audioAttributesFor(voiceUsage: ReminderVoiceUsage): AudioAttributes {
        val usage = when (voiceUsage) {
            ReminderVoiceUsage.NavigationGuidance -> AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE
            ReminderVoiceUsage.Assistant -> AudioAttributes.USAGE_ASSISTANT
            ReminderVoiceUsage.Media -> AudioAttributes.USAGE_MEDIA
        }
        return AudioAttributes.Builder()
            .setUsage(usage)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
    }

    private fun configureVoice(tts: TextToSpeech?): String {
        if (tts == null) return "vi"

        val selectedVoice = selectVoice(tts)
        if (selectedVoice != null) {
            val voiceResult = tts.setVoice(selectedVoice)
            Log.d(
                "TaskReminderReceiver",
                "selectedVoice=${selectedVoice.name}, locale=${selectedVoice.locale}, " +
                    "network=${selectedVoice.isNetworkConnectionRequired}, result=$voiceResult"
            )
            if (voiceResult == TextToSpeech.SUCCESS) {
                return selectedVoice.locale.language.orEmpty().ifBlank { "vi" }
            }
        }

        // Thử tiếng Việt trước
        val vietnameseLocale = Locale.Builder().setLanguage("vi").setRegion("VN").build()
        val result = tts.setLanguage(vietnameseLocale)
        Log.d("TaskReminderReceiver", "setLanguage vi-VN result=$result")
        if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
            return "vi"
        }

        // Fallback: locale mặc định của thiết bị
        val defaultLocale = Locale.getDefault()
        val defaultResult = tts.setLanguage(defaultLocale)
        Log.d("TaskReminderReceiver", "Fallback to default locale $defaultLocale result=$defaultResult")
        if (defaultResult != TextToSpeech.LANG_MISSING_DATA && defaultResult != TextToSpeech.LANG_NOT_SUPPORTED) {
            return defaultLocale.language.orEmpty().ifBlank { "en" }
        }

        // Last resort: tiếng Anh — hầu như mọi TTS engine đều hỗ trợ
        val englishResult = tts.setLanguage(Locale.ENGLISH)
        Log.d("TaskReminderReceiver", "Fallback to English result=$englishResult")
        return "en"
    }

    private fun selectVoice(tts: TextToSpeech): Voice? {
        val voices = runCatching { tts.voices }.getOrNull().orEmpty()
        if (voices.isEmpty()) {
            return null
        }

        val defaultLocale = Locale.getDefault()
        val defaultVoiceName = runCatching { tts.defaultVoice?.name }.getOrNull()
        val candidates = voices.map { voice ->
            ReminderVoiceCandidate(
                name = voice.name,
                language = voice.locale.language.orEmpty(),
                country = voice.locale.country.orEmpty(),
                isDefault = voice.name == defaultVoiceName ||
                    (defaultVoiceName == null && voice.locale == defaultLocale),
                requiresNetwork = voice.isNetworkConnectionRequired
            )
        }
        val selected = ReminderVoiceAudioPolicy.selectVoice(
            candidates = candidates,
            defaultLanguage = defaultLocale.language.orEmpty(),
            defaultCountry = defaultLocale.country.orEmpty()
        )
        return selected?.let { candidate -> voices.firstOrNull { it.name == candidate.name } }
    }

    private fun speechTextFor(language: String, title: String, note: String): String {
        val safeTitle = title.ifBlank { "Note" }
        return if (language.equals("vi", ignoreCase = true)) {
            if (note.isBlank()) "Thông báo: $safeTitle"
            else "Thông báo: $safeTitle. Nội dung: $note"
        } else if (note.isBlank()) {
            "Reminder: $safeTitle"
        } else {
            "Reminder: $safeTitle. Content: $note"
        }
    }

    private companion object {
        const val TTS_ATTEMPT_TIMEOUT_MS = 10_000L
    }
}
