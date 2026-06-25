@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.fini.todoapp.ui.home

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.fini.todoapp.data.model.CategoryResponse
import com.fini.todoapp.data.model.TaskRequest
import com.fini.todoapp.data.model.TaskResponse
import com.fini.todoapp.ui.ClearFocusLayer
import com.fini.todoapp.ui.theme.FiniBlack
import com.fini.todoapp.ui.theme.FiniBorder
import com.fini.todoapp.ui.theme.FiniGray
import com.fini.todoapp.ui.theme.FiniLightGray
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private enum class SpeechTarget {
    TITLE,
    NOTE
}

@Composable
fun CreateTaskScreen(
    categories: List<CategoryResponse>,
    task: TaskResponse? = null,
    onBack: () -> Unit,
    onCreate: (TaskRequest) -> Unit,
    onUpdate: (String, TaskRequest) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val taskKey = task?.id
    var title by remember(taskKey) { mutableStateOf(task?.title.orEmpty()) }
    var note by remember(taskKey) { mutableStateOf(task?.note.orEmpty()) }
    var priority by remember(taskKey) { mutableStateOf(task?.priority ?: false) }
    var categoryId by remember(taskKey) { mutableStateOf(task?.categoryId) }
    var notifyAt by remember(taskKey) { mutableStateOf(task?.notifyAt ?: task?.dueAt) }
    var notifyTime by remember(taskKey) {
        mutableStateOf(task?.notifyTime ?: timeFromApiValue(task?.notifyAt ?: task?.dueAt))
    }
    var autoTrashAfterNotification by remember(taskKey) {
        mutableStateOf(task?.autoTrashAfterNotification ?: false)
    }
    var repeatType by remember(taskKey) { mutableStateOf(task?.repeatType ?: "NONE") }
    var weeklyDays by remember(taskKey) { mutableStateOf(repeatDaysFromTask(task)) }
    var savedLocation by remember(taskKey) { mutableStateOf(locationFromTask(task)) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var speechError by remember { mutableStateOf<String?>(null) }
    var formError by remember { mutableStateOf<String?>(null) }
    var speechTarget by remember { mutableStateOf<SpeechTarget?>(null) }
    val isAutomotive = remember {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE) ||
        (context.getSystemService(android.content.Context.UI_MODE_SERVICE) as? android.app.UiModeManager)?.currentModeType == android.content.res.Configuration.UI_MODE_TYPE_CAR ||
        android.os.Build.DEVICE.contains("car", ignoreCase = true) ||
        android.os.Build.MODEL.contains("car", ignoreCase = true) ||
        android.os.Build.FINGERPRINT.contains("car", ignoreCase = true) ||
        (context.resources.configuration.screenWidthDp >= 800 && context.resources.configuration.screenWidthDp > context.resources.configuration.screenHeightDp)
    }
    val compact = isWideCompactTaskForm() || isAutomotive

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            return@rememberLauncherForActivityResult
        }

        val spokenText = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            ?.trim()

        if (spokenText.isNullOrBlank()) {
            speechError = "Không nhận được nội dung giọng nói."
            return@rememberLauncherForActivityResult
        }

        when (speechTarget) {
            SpeechTarget.TITLE -> title = spokenText
            SpeechTarget.NOTE -> {
                note = if (note.isBlank()) {
                    spokenText
                } else {
                    "$note\n$spokenText"
                }
            }
            null -> Unit
        }
        speechError = null
    }

    fun launchVietnameseSpeech(target: SpeechTarget) {
        speechTarget = target
        speechError = null
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Nói nội dung ghi chú")
        }

        runCatching {
            speechLauncher.launch(intent)
        }.onFailure {
            speechError = "Không mở được nhận giọng nói trên thiết bị này."
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            savedLocation = findLastKnownLocation(context)
            locationError = if (savedLocation == null) {
                "Chưa lấy được vị trí. Hãy bật GPS rồi thử lại."
            } else {
                null
            }
        } else {
            locationError = "Bạn cần cấp quyền vị trí để lưu địa điểm."
        }
    }

    fun saveTask() {
        val cleanTitle = title.trim()
        val cleanNote = note.trim()
        if (cleanTitle.isBlank() && cleanNote.isBlank()) {
            onBack()
            return
        }

        val cleanNotifyAt = if (repeatType == "NONE") notifyAt else null
        val cleanNotifyTime = if (repeatType == "NONE") {
            null
        } else {
            notifyTime ?: currentTimeValue()
        }
        val cleanAutoTrashAfterNotification = repeatType == "NONE" && autoTrashAfterNotification

        val isNotifyAtChanged = task == null || cleanNotifyAt != (task.notifyAt ?: task.dueAt)
        if (cleanNotifyAt != null && isNotifyAtChanged && isPastApiDateTime(cleanNotifyAt)) {
            formError = "Chỉ được chọn thời gian hiện tại hoặc tương lai."
            return
        }

        val request = TaskRequest(
            categoryId = categoryId,
            title = cleanTitle.ifBlank { cleanNote.lineSequence().firstOrNull()?.take(80) ?: "Ghi chú mới" },
            note = cleanNote.ifBlank { null },
            priority = priority,
            notifyAt = cleanNotifyAt,
            notifyTime = cleanNotifyTime,
            autoTrashAfterNotification = cleanAutoTrashAfterNotification,
            dueAt = cleanNotifyAt,
            reminderEnabled = cleanNotifyAt != null || cleanNotifyTime != null,
            repeatType = repeatType,
            repeatDays = if (repeatType == "WEEKLY") weeklyDays.joinToString(",") else null,
            hasLocation = savedLocation != null,
            latitude = savedLocation?.latitude,
            longitude = savedLocation?.longitude,
            locationName = if (savedLocation != null) "Vị trí đã lưu" else null,
            address = task?.address
        )

        if (task == null) {
            onCreate(request)
        } else {
            onUpdate(task.id, request)
        }
        onBack()
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        color = Color.White
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            ClearFocusLayer()

            val formMaxWidth = if (compact) 760.dp else maxWidth

            Column(modifier = Modifier.fillMaxSize()) {
                CreateTaskTopBar(
                    title = title,
                    compact = compact,
                    onTitleChange = { title = it },
                    onVoiceTitle = { launchVietnameseSpeech(SpeechTarget.TITLE) },
                    onBack = onBack,
                    onSave = ::saveTask
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            horizontal = if (compact) 16.dp else 24.dp,
                            vertical = if (compact) 12.dp else 20.dp
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = formMaxWidth)
                            .fillMaxWidth()
                            .animateContentSize(animationSpec = tween(300)),
                        verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 22.dp)
                    ) {
                        NoteBodyEditor(
                            note = note,
                            compact = compact,
                            onVoiceNote = { launchVietnameseSpeech(SpeechTarget.NOTE) },
                            onNoteChange = { note = it }
                        )

                        if (speechError != null) {
                            Text(
                                text = speechError.orEmpty(),
                                color = FiniGray,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        CategoryPicker(
                            categories = categories,
                            selectedCategoryId = categoryId,
                            onSelected = { categoryId = it }
                        )

                        PrioritySection(
                            priority = priority,
                            onPriorityChange = { priority = it }
                        )

                        RepeatSection(
                            repeatType = repeatType,
                            weeklyDays = weeklyDays,
                            compact = compact,
                            onRepeatTypeChange = {
                                repeatType = it
                                if (it != "NONE") {
                                    autoTrashAfterNotification = false
                                }
                                if (it != "NONE" && notifyTime == null) {
                                    notifyTime = currentTimeValue()
                                }
                            },
                            onToggleDay = { day ->
                                weeklyDays = if (weeklyDays.contains(day)) {
                                    weeklyDays - day
                                } else {
                                    weeklyDays + day
                                }
                                if (weeklyDays.isEmpty()) {
                                    weeklyDays = setOf(day)
                                }
                            }
                        )

                        NotificationTimeSection(
                            notifyAt = notifyAt,
                            notifyTime = notifyTime,
                            repeatType = repeatType,
                            compact = compact,
                            onNotifyAtSelected = {
                                notifyAt = it
                                formError = null
                            },
                            onNotifyTimeSelected = {
                                notifyTime = it
                                formError = null
                            },
                            onError = { formError = it }
                        )

                        AutoTrashSection(
                            checked = autoTrashAfterNotification,
                            enabled = repeatType == "NONE",
                            onCheckedChange = { autoTrashAfterNotification = it }
                        )

                        if (formError != null) {
                            Text(
                                text = formError.orEmpty(),
                                color = FiniGray,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        LocationSection(
                            location = savedLocation,
                            error = locationError,
                            compact = compact,
                            showDirections = task != null,
                            onSaveLocation = {
                                if (hasLocationPermission(context)) {
                                    savedLocation = findLastKnownLocation(context)
                                    locationError = if (savedLocation == null) {
                                        "Chưa lấy được vị trí. Hãy bật GPS rồi thử lại."
                                    } else {
                                        null
                                    }
                                } else {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(if (compact) 8.dp else 18.dp))
                    }
                }
            }
        }
    }

}

@Composable
private fun CreateTaskTopBar(
    title: String,
    compact: Boolean,
    onTitleChange: (String) -> Unit,
    onVoiceTitle: () -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    val titleStyle = if (compact) {
        MaterialTheme.typography.titleLarge
    } else {
        MaterialTheme.typography.headlineMedium
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compact) 54.dp else 76.dp)
            .border(BorderStroke(1.dp, FiniBorder))
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = if (compact) 760.dp else maxWidth)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Quay lại",
                    tint = FiniBlack
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                if (title.isBlank()) {
                    Text(
                        text = "Tiêu đề",
                        style = titleStyle,
                        color = FiniGray,
                        fontWeight = FontWeight.Bold
                    )
                }
                BasicTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    singleLine = true,
                    textStyle = titleStyle.copy(
                        color = FiniBlack,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            IconButton(onClick = onVoiceTitle) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Nhập tiêu đề bằng giọng nói",
                    tint = FiniGray
                )
            }

            IconButton(onClick = onSave) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Lưu",
                    tint = FiniBlack
                )
            }
        }
    }
}

@Composable
private fun NoteBodyEditor(
    note: String,
    compact: Boolean,
    onVoiceNote: () -> Unit,
    onNoteChange: (String) -> Unit
) {
    val textStyle = if (compact) {
        MaterialTheme.typography.bodyLarge
    } else {
        MaterialTheme.typography.titleLarge
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compact) 132.dp else 220.dp)
    ) {
        if (note.isBlank()) {
            Text(
                text = "Nội dung",
                color = FiniGray,
                style = textStyle
            )
        }
        BasicTextField(
            value = note,
            onValueChange = onNoteChange,
            textStyle = TextStyle(
                color = FiniBlack,
                fontSize = textStyle.fontSize,
                lineHeight = textStyle.lineHeight
            ),
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 38.dp, bottom = 36.dp)
        )

        IconButton(
            onClick = onVoiceNote,
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Nhập nội dung bằng giọng nói",
                tint = FiniGray
            )
        }
    }
}

@Composable
private fun CategoryPicker(
    categories: List<CategoryResponse>,
    selectedCategoryId: String?,
    onSelected: (String?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Phân loại",
            color = FiniBlack,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CategoryChoiceChip(
                text = "Không",
                selected = selectedCategoryId == null,
                color = FiniBlack,
                showDotWhenUnselected = false,
                onClick = { onSelected(null) }
            )
            categories.forEach { category ->
                val categoryColor = colorFromHex(category.color)
                val selected = selectedCategoryId == category.id
                CategoryChoiceChip(
                    text = category.name,
                    selected = selected,
                    color = categoryColor,
                    showDotWhenUnselected = true,
                    onClick = { onSelected(category.id) }
                )
            }
        }
    }
}

@Composable
private fun CategoryChoiceChip(
    text: String,
    selected: Boolean,
    color: Color,
    showDotWhenUnselected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .height(32.dp)
            .animateContentSize(animationSpec = tween(220)),
        shape = MaterialTheme.shapes.medium,
        color = Color.White,
        border = if (selected) null else BorderStroke(1.dp, FiniBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selected || showDotWhenUnselected) {
                CategorySelectionMark(
                    selected = selected,
                    color = color,
                    showDotWhenUnselected = showDotWhenUnselected
                )
            }
            Text(
                text = text,
                color = color,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CategorySelectionMark(
    selected: Boolean,
    color: Color,
    showDotWhenUnselected: Boolean
) {
    Box(
        modifier = Modifier.size(16.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = selected,
            transitionSpec = {
                (fadeIn(tween(120)) + scaleIn(tween(120), initialScale = 0.65f))
                    .togetherWith(fadeOut(tween(90)) + scaleOut(tween(90), targetScale = 0.65f))
                    .using(SizeTransform(clip = false))
            },
            label = "category-selection-mark"
        ) { isSelected ->
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            } else if (showDotWhenUnselected) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .background(color, CircleShape)
                )
            } else {
                Spacer(modifier = Modifier.size(1.dp))
            }
        }
    }
}

@Composable
private fun PrioritySection(
    priority: Boolean,
    onPriorityChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = priority,
            onCheckedChange = onPriorityChange
        )
        Text(
            text = "Ưu tiên",
            color = FiniBlack,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun NotificationTimeSection(
    notifyAt: String?,
    notifyTime: String?,
    repeatType: String,
    compact: Boolean,
    onNotifyAtSelected: (String?) -> Unit,
    onNotifyTimeSelected: (String?) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Thông báo",
            style = MaterialTheme.typography.labelLarge,
            color = FiniBlack
        )
        Surface(
            onClick = {
                showNotificationPicker(
                    context = context,
                    repeatType = repeatType,
                    currentNotifyAt = notifyAt,
                    currentNotifyTime = notifyTime,
                    onNotifyAtSelected = onNotifyAtSelected,
                    onNotifyTimeSelected = onNotifyTimeSelected,
                    onError = onError
                )
            },
            modifier = Modifier
                .fillMaxWidth(if (compact) 0.5f else 1f)
                .height(if (compact) 42.dp else 58.dp),
            shape = MaterialTheme.shapes.medium,
            color = Color.White,
            border = BorderStroke(1.dp, FiniBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = FiniGray,
                    modifier = Modifier.size(if (compact) 16.dp else 20.dp)
                )
                Text(
                    text = notificationFieldText(repeatType, notifyAt, notifyTime),
                    color = if (hasNotificationValue(repeatType, notifyAt, notifyTime)) FiniBlack else FiniGray,
                    style = if (compact) {
                        MaterialTheme.typography.bodySmall
                    } else {
                        MaterialTheme.typography.bodyMedium
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun AutoTrashSection(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
        Text(
            text = "Bỏ vào thùng rác sau khi thông báo",
            color = if (enabled) FiniBlack else FiniGray,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ReminderSection(
    enabled: Boolean,
    canEnable: Boolean,
    minutes: Int,
    compact: Boolean,
    onSwitch: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FiniLightGray, MaterialTheme.shapes.medium)
            .padding(
                horizontal = if (compact) 12.dp else 14.dp,
                vertical = if (compact) 8.dp else 12.dp
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = if (canEnable) FiniBlack else FiniGray,
                modifier = Modifier.size(if (compact) 18.dp else 24.dp)
            )
            Column {
                Text(
                    text = "Nhắc trước",
                    color = if (canEnable) FiniBlack else FiniGray,
                    style = if (compact) {
                        MaterialTheme.typography.bodyMedium
                    } else {
                        MaterialTheme.typography.titleMedium
                    }
                )
                if (enabled) {
                    Text(
                        text = "$minutes phút trước kết thúc",
                        color = FiniGray,
                        style = if (compact) {
                            MaterialTheme.typography.bodySmall
                        } else {
                            MaterialTheme.typography.bodyMedium
                        }
                    )
                }
            }
        }
        Switch(
            checked = enabled,
            enabled = canEnable,
            onCheckedChange = onSwitch
        )
    }
}

@Composable
private fun RepeatSection(
    repeatType: String,
    weeklyDays: Set<String>,
    compact: Boolean,
    onRepeatTypeChange: (String) -> Unit,
    onToggleDay: (String) -> Unit
) {
    Column {
        Text(
            text = "Lặp lại",
            style = MaterialTheme.typography.labelLarge,
            color = FiniBlack
        )
        Spacer(modifier = Modifier.height(if (compact) 8.dp else 12.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RepeatPill("None", repeatType == "NONE") { onRepeatTypeChange("NONE") }
            RepeatPill("Daily", repeatType == "DAILY") { onRepeatTypeChange("DAILY") }
            RepeatPill("Weekly", repeatType == "WEEKLY") { onRepeatTypeChange("WEEKLY") }
        }

        AnimatedVisibility(
            visible = repeatType == "WEEKLY",
            enter = expandVertically(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
            exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
        ) {
            Column {
                Spacer(modifier = Modifier.height(if (compact) 8.dp else 12.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(
                        "MON" to "T2",
                        "TUE" to "T3",
                        "WED" to "T4",
                        "THU" to "T5",
                        "FRI" to "T6",
                        "SAT" to "T7",
                        "SUN" to "CN"
                    ).forEach { (value, label) ->
                        DayCircle(
                            label = label,
                            selected = weeklyDays.contains(value),
                            compact = compact,
                            onClick = { onToggleDay(value) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RepeatPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) },
        leadingIcon = if (selected) {
            {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            null
        }
    )
}

@Composable
private fun DayCircle(
    label: String,
    selected: Boolean,
    compact: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(if (compact) 34.dp else 44.dp),
        shape = CircleShape,
        color = if (selected) FiniBlack else Color.White,
        border = BorderStroke(1.dp, if (selected) FiniBlack else FiniBorder)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = if (selected) Color.White else FiniBlack,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun LocationSection(
    location: Location?,
    error: String?,
    compact: Boolean,
    showDirections: Boolean,
    onSaveLocation: () -> Unit
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MonoActionButton(
            text = "Lưu vị trí hiện tại",
            onClick = onSaveLocation,
            height = if (compact) 42.dp else 52.dp,
            icon = Icons.Default.LocationOn
        )

        if (location != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = FiniLightGray,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Vị trí đã lưu",
                        color = FiniBlack,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "%.5f, %.5f".format(location.latitude, location.longitude),
                        color = FiniGray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (showDirections) {
                MonoActionButton(
                    text = "Chỉ đường",
                    icon = Icons.Default.NearMe,
                    height = if (compact) 42.dp else 52.dp,
                    onClick = {
                        val uri = Uri.parse(
                            "google.navigation:q=${location.latitude},${location.longitude}"
                        )
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    }
                )
            }
        }

        if (error != null) {
            Text(
                text = error,
                color = FiniGray,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ReminderPickerDialog(
    selected: Int,
    onDismiss: () -> Unit,
    onSelected: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nhắc trước") },
        text = {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(5, 10, 15, 20, 25, 30).forEach { minute ->
                    FilterChip(
                        selected = selected == minute,
                        onClick = { onSelected(minute) },
                        label = { Text("${minute}p") }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Xong")
            }
        }
    )
}

private fun showDateTimePicker(
    context: Context,
    currentValue: String?,
    onSelected: (String) -> Unit
) {
    val calendar = calendarFromApiValue(currentValue) ?: Calendar.getInstance()
    DatePickerDialog(
        context,
        { _, year, month, day ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, day)

            TimePickerDialog(
                context,
                { _, hour, minute ->
                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                    calendar.set(Calendar.MINUTE, minute)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    onSelected(apiFormat().format(calendar.time))
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}

private fun showNotificationPicker(
    context: Context,
    repeatType: String,
    currentNotifyAt: String?,
    currentNotifyTime: String?,
    onNotifyAtSelected: (String?) -> Unit,
    onNotifyTimeSelected: (String?) -> Unit,
    onError: (String) -> Unit
) {
    if (repeatType != "NONE") {
        val calendar = calendarFromTimeValue(currentNotifyTime) ?: Calendar.getInstance()
        TimePickerDialog(
            context,
            { _, hour, minute ->
                onNotifyTimeSelected("%02d:%02d:00".format(hour, minute))
                onNotifyAtSelected(null)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
        return
    }

    val calendar = calendarFromApiValue(currentNotifyAt) ?: Calendar.getInstance()
    val dialog = DatePickerDialog(
        context,
        { _, year, month, day ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, day)

            TimePickerDialog(
                context,
                { _, hour, minute ->
                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                    calendar.set(Calendar.MINUTE, minute)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)

                    if (calendar.before(Calendar.getInstance())) {
                        onError("Chỉ được chọn thời gian hiện tại hoặc tương lai.")
                    } else {
                        onNotifyAtSelected(apiFormat().format(calendar.time))
                        onNotifyTimeSelected(null)
                    }
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )
    dialog.datePicker.minDate = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    dialog.show()
}

private fun notificationFieldText(
    repeatType: String,
    notifyAt: String?,
    notifyTime: String?
): String {
    return if (repeatType == "NONE") {
        notifyAt?.let(::compactDateTime) ?: "Thông báo cho tôi vào lúc"
    } else {
        notifyTime?.let { "Thông báo lúc ${compactTimeValue(it)}" } ?: "Chọn giờ thông báo"
    }
}

private fun hasNotificationValue(
    repeatType: String,
    notifyAt: String?,
    notifyTime: String?
): Boolean {
    return if (repeatType == "NONE") notifyAt != null else notifyTime != null
}

private fun currentTimeValue(): String {
    val calendar = Calendar.getInstance()
    return "%02d:%02d:00".format(
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE)
    )
}

private fun timeFromApiValue(value: String?): String? {
    val calendar = calendarFromApiValue(value) ?: return null
    return "%02d:%02d:00".format(
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE)
    )
}

private fun calendarFromTimeValue(value: String?): Calendar? {
    val parts = value.orEmpty().split(":")
    if (parts.size < 2) {
        return null
    }
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    return Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}

private fun isPastApiDateTime(value: String): Boolean {
    return calendarFromApiValue(value)?.before(Calendar.getInstance()) == true
}

private fun compactTimeValue(value: String): String {
    val parts = value.split(":")
    if (parts.size < 2) {
        return value
    }
    return "${parts[0].padStart(2, '0')}:${parts[1].padStart(2, '0')}"
}

private fun normalizedStartForRepeat(
    startAt: String?,
    dueAt: String?,
    repeatType: String
): String? {
    if (repeatType == "NONE") {
        return startAt
    }
    return startAt ?: midnightOf(dueAt) ?: todayMidnight()
}

private fun normalizedDueForRepeat(
    startAt: String?,
    dueAt: String?,
    repeatType: String
): String? {
    if (repeatType == "NONE") {
        return dueAt
    }
    return dueAt ?: startAt ?: todayMidnight()
}

private fun midnightOf(value: String?): String? {
    val calendar = calendarFromApiValue(value) ?: return null
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return apiFormat().format(calendar.time)
}

private fun todayMidnight(): String {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return apiFormat().format(calendar.time)
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

private fun compactDateTime(value: String): String {
    return value.replace("T", " ").removeSuffix(":00")
}

private fun apiFormat(): SimpleDateFormat {
    return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
}

private fun hasLocationPermission(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    return fine || coarse
}

private fun findLastKnownLocation(context: Context): Location? {
    return runCatching {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        manager.getProviders(true)
            .mapNotNull { provider ->
                runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
            }
            .maxByOrNull { it.time }
    }.getOrNull()
}

private fun repeatDaysFromTask(task: TaskResponse?): Set<String> {
    if (task?.repeatType != "WEEKLY") {
        return setOf("MON")
    }

    val days = task.repeatDays.orEmpty()
        .split(",", " ")
        .mapNotNull { token ->
            when (token.trim().uppercase()) {
                "1", "MON", "MONDAY" -> "MON"
                "2", "TUE", "TUESDAY" -> "TUE"
                "3", "WED", "WEDNESDAY" -> "WED"
                "4", "THU", "THURSDAY" -> "THU"
                "5", "FRI", "FRIDAY" -> "FRI"
                "6", "SAT", "SATURDAY" -> "SAT"
                "7", "SUN", "SUNDAY" -> "SUN"
                else -> null
            }
        }
        .toSet()

    return days.ifEmpty { setOf("MON") }
}

private fun locationFromTask(task: TaskResponse?): Location? {
    if (task?.hasLocation != true || task.latitude == null || task.longitude == null) {
        return null
    }

    return Location("saved-task").apply {
        latitude = task.latitude
        longitude = task.longitude
    }
}

@Composable
private fun isWideCompactTaskForm(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.screenWidthDp >= 700 && configuration.screenHeightDp <= 700
}

private fun colorFromHex(hex: String?): Color {
    return runCatching {
        Color(android.graphics.Color.parseColor(hex ?: "#111111"))
    }.getOrDefault(FiniBlack)
}
