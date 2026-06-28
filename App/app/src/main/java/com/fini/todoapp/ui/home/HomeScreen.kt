package com.fini.todoapp.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.LocalIndication
import androidx.compose.material3.ripple
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fini.todoapp.data.model.CategoryResponse
import com.fini.todoapp.data.model.TaskResponse
import com.fini.todoapp.notification.TaskNotificationScheduler
import com.fini.todoapp.ui.ClearFocusLayer
import com.fini.todoapp.ui.theme.FiniBlack
import com.fini.todoapp.ui.theme.FiniBorder
import com.fini.todoapp.ui.theme.FiniGray
import com.fini.todoapp.ui.theme.FiniLightGray
import com.fini.todoapp.ui.theme.FiniSurface
import android.content.pm.PackageManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private data class TaskSection(
    val title: String,
    val tasks: List<TaskResponse>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onCreateTask: () -> Unit,
    onOpenTask: (String) -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit
) {
    val state = viewModel.state
    var showCreateCategoryDialog by remember { mutableStateOf(false) }
    var pendingDeleteCategory by remember { mutableStateOf<CategoryResponse?>(null) }
    var showConfirmClearTrashDialog by remember { mutableStateOf(false) }
    val compact = isWideCompactHome()
    val context = LocalContext.current
    val gridState = rememberLazyGridState()

    LaunchedEffect(state.scrollToTop) {
        if (state.scrollToTop) {
            gridState.animateScrollToItem(0)
            viewModel.resetScrollToTop()
        }
    }
    val selectedCategoryName = state.categories.firstOrNull { it.id == state.selectedCategoryId }?.name
    val headerTitle = when {
        state.dateFilter == "TRASH" -> "Thùng rác"
        selectedCategoryName != null -> selectedCategoryName
        else -> "Tất cả ghi chú"
    }

    LaunchedEffect(Unit) {
        viewModel.loadAll()
    }

    LaunchedEffect(state.allTasks, state.dateFilter) {
        if (state.dateFilter != "TRASH") {
            TaskNotificationScheduler.scheduleAll(context, state.allTasks)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        containerColor = Color.White,
        floatingActionButton = {
            val fabSize = if (compact) 52.dp else 58.dp
            val fabShadowSize = fabSize + 18.dp

            Box(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = if (compact) 8.dp else 12.dp, end = 8.dp)
                    .size(fabShadowSize)
                    .drawBehind {
                        val buttonRadius = fabSize.toPx() / 2f
                        val shadowPaint = android.graphics.Paint().apply {
                            isAntiAlias = true
                            color = android.graphics.Color.WHITE
                            setShadowLayer(
                                13.dp.toPx(),
                                0f,
                                0f,
                                android.graphics.Color.argb(58, 0, 0, 0)
                            )
                        }
                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawCircle(
                                center.x,
                                center.y,
                                buttonRadius - 1.dp.toPx(),
                                shadowPaint
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    onClick = onCreateTask,
                    modifier = Modifier.size(fabSize),
                    shape = CircleShape,
                    color = Color.White,
                    contentColor = FiniBlack,
                    shadowElevation = 0.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Tạo ghi chú",
                            tint = FiniBlack,
                            modifier = Modifier.size(if (compact) 24.dp else 28.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ClearFocusLayer()

            val context = LocalContext.current
            val isAutomotive = remember {
                context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE) ||
                (context.getSystemService(android.content.Context.UI_MODE_SERVICE) as? android.app.UiModeManager)?.currentModeType == android.content.res.Configuration.UI_MODE_TYPE_CAR ||
                android.os.Build.DEVICE.contains("car", ignoreCase = true) ||
                android.os.Build.MODEL.contains("car", ignoreCase = true) ||
                android.os.Build.FINGERPRINT.contains("car", ignoreCase = true) ||
                (context.resources.configuration.screenWidthDp >= 800 && context.resources.configuration.screenWidthDp > context.resources.configuration.screenHeightDp)
            }

            val landscape = LocalConfiguration.current.screenWidthDp > LocalConfiguration.current.screenHeightDp
            val gridCells = GridCells.Adaptive(
                minSize = when {
                    isAutomotive -> 260.dp
                    compact -> 230.dp
                    landscape -> 190.dp
                    else -> 170.dp
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (compact) 18.dp else 24.dp)
            ) {
            HomeHeader(
                title = headerTitle,
                noteCount = state.tasks.size,
                compact = compact,
                isAutomotive = isAutomotive,
                state = state,
                onFilterSelected = viewModel::setDateFilter,
                onOpenTrash = viewModel::openTrash,
                onRefresh = onRefresh,
                onClearTrash = { showConfirmClearTrashDialog = true },
                onLogout = onLogout
            )

            val baseSpacing = if (compact) 16.dp else 24.dp

            Spacer(modifier = Modifier.height(baseSpacing))

            SearchRow(
                keyword = state.keyword,
                compact = compact,
                onKeywordChange = viewModel::setKeyword
            )

            Spacer(modifier = Modifier.height(baseSpacing - 4.dp))

            CategoryFilterRow(
                categories = state.categories,
                selectedCategoryId = state.selectedCategoryId,
                compact = compact,
                onSelected = viewModel::setCategoryFilter,
                onCreateCategory = { showCreateCategoryDialog = true },
                onDeleteCategoryRequest = { category ->
                    pendingDeleteCategory = category
                }
            )

            Spacer(modifier = Modifier.height(baseSpacing - 4.dp))

            if (state.error != null) {
                ErrorBanner(text = state.error)
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (state.tasks.isEmpty() && !state.loading) {
                EmptyNotes(
                    isTrash = state.dateFilter == "TRASH",
                    modifier = Modifier.weight(1f)
                )
            } else {
                val sections = buildTaskSections(
                    tasks = state.tasks,
                    dateFilter = state.dateFilter
                )

                LazyVerticalGrid(
                    columns = gridCells,
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp),
                    verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp),
                    content = {
                        sections.forEach { section ->
                            item(
                                key = "section-${section.title}",
                                span = { GridItemSpan(maxLineSpan) }
                            ) {
                                SectionTitle(
                                    text = section.title,
                                    compact = compact,
                                    isAutomotive = isAutomotive
                                )
                            }

                            items(
                                items = section.tasks,
                                key = { it.id }
                            ) { task ->
                                NoteCard(
                                    task = task,
                                    categories = state.categories,
                                    compact = compact,
                                    isAutomotive = isAutomotive,
                                    trashMode = state.dateFilter == "TRASH",
                                    onOpen = { onOpenTask(task.id) },
                                    onTogglePriority = { viewModel.togglePriority(task) },
                                    onDelete = {
                                        if (state.dateFilter == "TRASH") {
                                            viewModel.restoreTask(task.id)
                                        } else {
                                            viewModel.deleteTask(task.id)
                                        }
                                    }
                                )
                            }
                        }
                    }
                )
            }
            }
        }
    }

    if (showCreateCategoryDialog) {
        CreateCategoryDialog(
            onDismiss = { showCreateCategoryDialog = false },
            onCreate = { name, color ->
                viewModel.createCategory(name, color)
                showCreateCategoryDialog = false
            }
        )
    }

    pendingDeleteCategory?.let { category ->
        val noteCount = state.allTasks.count { it.categoryId == category.id }

        val deleteDialogAnimateTrigger = remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            deleteDialogAnimateTrigger.value = true
        }
        val deleteDialogScale by animateFloatAsState(
            targetValue = if (deleteDialogAnimateTrigger.value) 1f else 0.92f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            label = "delete-dialog-scale"
        )
        val deleteDialogAlpha by animateFloatAsState(
            targetValue = if (deleteDialogAnimateTrigger.value) 1f else 0f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            label = "delete-dialog-alpha"
        )

        val deleteCancelInteractionSource = remember { MutableInteractionSource() }
        val deleteCancelIsPressed by deleteCancelInteractionSource.collectIsPressedAsState()
        val deleteCancelScale by animateFloatAsState(
            targetValue = if (deleteCancelIsPressed) 0.92f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            label = "delete-cancel-scale"
        )

        Dialog(
            onDismissRequest = { pendingDeleteCategory = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .width(344.dp)
                    .graphicsLayer(scaleX = deleteDialogScale, scaleY = deleteDialogScale, alpha = deleteDialogAlpha),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                shadowElevation = 18.dp
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Xoá category",
                        color = FiniBlack,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (noteCount > 0) {
                            "Trong category này hiện đang có $noteCount ghi chú, bạn có chắc muốn xoá?"
                        } else {
                            "Bạn có chắc muốn xoá category \"${category.name}\"?"
                        },
                        color = FiniBlack,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = { pendingDeleteCategory = null },
                            modifier = Modifier
                                .width(96.dp)
                                .height(38.dp)
                                .graphicsLayer(scaleX = deleteCancelScale, scaleY = deleteCancelScale),
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, FiniBlack),
                            interactionSource = deleteCancelInteractionSource
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Huỷ",
                                    color = FiniBlack,
                                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        MonoActionButton(
                            text = "Xoá",
                            modifier = Modifier.width(96.dp),
                            fillMaxWidth = false,
                            onClick = {
                                viewModel.deleteCategory(category.id)
                                pendingDeleteCategory = null
                            },
                            height = 38.dp
                        )
                    }
                }
            }
        }
    }

    if (showConfirmClearTrashDialog) {
        val confirmAnimateTrigger = remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            confirmAnimateTrigger.value = true
        }
        val confirmScale by animateFloatAsState(
            targetValue = if (confirmAnimateTrigger.value) 1f else 0.92f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            label = "confirm-dialog-scale"
        )
        val confirmAlpha by animateFloatAsState(
            targetValue = if (confirmAnimateTrigger.value) 1f else 0f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            label = "confirm-dialog-alpha"
        )

        val confirmCancelInteractionSource = remember { MutableInteractionSource() }
        val confirmCancelIsPressed by confirmCancelInteractionSource.collectIsPressedAsState()
        val confirmCancelScale by animateFloatAsState(
            targetValue = if (confirmCancelIsPressed) 0.92f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            label = "confirm-cancel-scale"
        )

        Dialog(
            onDismissRequest = { showConfirmClearTrashDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .width(344.dp)
                    .graphicsLayer(scaleX = confirmScale, scaleY = confirmScale, alpha = confirmAlpha),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                shadowElevation = 18.dp
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Xoá sạch thùng rác?",
                        color = FiniBlack,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Hành động này sẽ xoá vĩnh viễn tất cả các ghi chú trong thùng rác và không thể khôi phục lại. Bạn có chắc chắn?",
                        color = FiniBlack,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = { showConfirmClearTrashDialog = false },
                            modifier = Modifier
                                .width(96.dp)
                                .height(38.dp)
                                .graphicsLayer(scaleX = confirmCancelScale, scaleY = confirmCancelScale),
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, FiniBlack),
                            interactionSource = confirmCancelInteractionSource
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Huỷ",
                                    color = FiniBlack,
                                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        MonoActionButton(
                            text = "Xoá sạch",
                            modifier = Modifier.width(96.dp),
                            fillMaxWidth = false,
                            onClick = {
                                viewModel.clearTrash()
                                showConfirmClearTrashDialog = false
                            },
                            height = 38.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(
    title: String,
    noteCount: Int,
    compact: Boolean,
    isAutomotive: Boolean = false,
    state: HomeUiState,
    onFilterSelected: (String) -> Unit,
    onOpenTrash: () -> Unit,
    onRefresh: () -> Unit,
    onClearTrash: () -> Unit,
    onLogout: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = if (isAutomotive) 36.dp else if (compact) 12.dp else 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = if (isAutomotive) {
                MaterialTheme.typography.headlineLarge
            } else if (compact) {
                MaterialTheme.typography.titleLarge
            } else {
                MaterialTheme.typography.headlineMedium
            },
            color = FiniBlack,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(if (isAutomotive) 6.dp else if (compact) 2.dp else 4.dp))
        Text(
            text = "$noteCount ghi chú",
            style = if (isAutomotive) {
                MaterialTheme.typography.titleLarge
            } else if (compact) {
                MaterialTheme.typography.bodySmall
            } else {
                MaterialTheme.typography.titleMedium
            },
            color = FiniGray
        )

        Spacer(modifier = Modifier.height(if (isAutomotive) 38.dp else if (compact) 14.dp else 32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Lọc",
                        tint = FiniBlack
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.width(138.dp),
                    shape = RoundedCornerShape(10.dp),
                    containerColor = Color.White,
                    tonalElevation = 0.dp,
                    shadowElevation = 10.dp
                ) {
                    FilterMenuItem("Tất cả", state.dateFilter == "ALL", onClick = {
                        onFilterSelected("ALL")
                    })
                    FilterMenuItem("Hôm nay", state.dateFilter == "TODAY", onClick = {
                        onFilterSelected("TODAY")
                    })
                    FilterMenuItem("Tuần này", state.dateFilter == "THIS_WEEK", onClick = {
                        onFilterSelected("THIS_WEEK")
                    })
                    FilterMenuItem("Ưu tiên", state.dateFilter == "PRIORITY", onClick = {
                        onFilterSelected("PRIORITY")
                    })
                    FilterMenuItem("Thùng rác", state.dateFilter == "TRASH", onClick = {
                        onOpenTrash()
                    })
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.dateFilter == "TRASH" && state.tasks.isNotEmpty()) {
                    IconButton(onClick = onClearTrash) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Xoá sạch thùng rác",
                            tint = Color(0xFFD92D20)
                        )
                    }
                }
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = FiniBlack
                    )
                }
                IconButton(onClick = onLogout) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Đăng xuất",
                        tint = FiniBlack
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterMenuItem(
    text: String,
    selected: Boolean,
    showCheck: Boolean = true,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) Color(0xFFEDEDED) else Color.White,
        animationSpec = tween(260),
        label = "filter-menu-background"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "filter-menu-item-scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .background(backgroundColor)
            .clickable(
                role = Role.Button,
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                color = FiniBlack,
                style = MaterialTheme.typography.bodySmall
            )
            AnimatedContent(
                targetState = selected && showCheck,
                transitionSpec = {
                    (fadeIn(tween(220)) +
                            scaleIn(tween(220), initialScale = 0.65f) +
                            slideInHorizontally(tween(220)) { it / 2 })
                        .togetherWith(
                            fadeOut(tween(160)) +
                                    scaleOut(tween(160), targetScale = 0.65f) +
                                    slideOutHorizontally(tween(160)) { it / 2 }
                        )
                        .using(SizeTransform(clip = false))
                },
                label = "filter-menu-check"
            ) { checked ->
                if (checked) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = FiniBlack,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchRow(
    keyword: String,
    compact: Boolean,
    onKeywordChange: (String) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = MaterialTheme.shapes.medium

    BasicTextField(
        value = keyword,
        onValueChange = onKeywordChange,
        modifier = Modifier.fillMaxWidth(),
        interactionSource = interactionSource,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = FiniBlack,
            fontSize = if (compact) 13.sp else 14.sp,
            lineHeight = if (compact) 13.sp else 14.sp,
            platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                includeFontPadding = false
            )
        ),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(FiniBlack),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (compact) 40.dp else 48.dp)
                    .background(Color.White, shape)
                    .border(1.dp, FiniBorder, shape)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = FiniGray,
                    modifier = Modifier.size(if (compact) 14.dp else 20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (keyword.isEmpty()) {
                        Text(
                            text = "Tìm kiếm",
                            color = FiniGray,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = if (compact) 13.sp else 14.sp,
                                lineHeight = if (compact) 13.sp else 14.sp,
                                platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                                    includeFontPadding = false
                                )
                            )
                        )
                    }
                    innerTextField()
                }
            }
        }
    )
}

@Composable
private fun DateFilterRow(
    selected: String,
    onSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MonoFilterChip("All", selected == "ALL") { onSelected("ALL") }
        MonoFilterChip("Today", selected == "TODAY") { onSelected("TODAY") }
        MonoFilterChip("This week", selected == "THIS_WEEK") { onSelected("THIS_WEEK") }
    }
}

@Composable
private fun CategoryFilterRow(
    categories: List<CategoryResponse>,
    selectedCategoryId: String?,
    compact: Boolean = false,
    onSelected: (String?) -> Unit,
    onCreateCategory: () -> Unit,
    onDeleteCategoryRequest: (CategoryResponse) -> Unit
) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MonoFilterChip("+ Category", false, compact, onCreateCategory)
        AllCategoryChip(selectedCategoryId == null, compact) { onSelected(null) }
        categories.forEach { category ->
            CategoryChip(
                category = category,
                selected = selectedCategoryId == category.id,
                compact = compact,
                onClick = { onSelected(category.id) },
                onDeleteRequest = { onDeleteCategoryRequest(category) }
            )
        }
    }
}

@Composable
private fun AllCategoryChip(
    selected: Boolean,
    compact: Boolean = false,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(if (compact) 10.dp else 8.dp)
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = Modifier
            .height(if (compact) 40.dp else 32.dp)
            .animateContentSize(animationSpec = tween(220))
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            ),
        shape = shape,
        color = Color.White,
        border = if (selected) null else BorderStroke(1.dp, FiniBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (compact) 16.dp else 12.dp),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(
                visible = selected,
                enter = fadeIn(tween(120)) + scaleIn(tween(120), initialScale = 0.65f),
                exit = fadeOut(tween(90)) + scaleOut(tween(90), targetScale = 0.65f)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = FiniBlack,
                    modifier = Modifier.size(if (compact) 18.dp else 16.dp)
                )
            }
            Text(
                text = "Tất cả",
                color = FiniBlack,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Normal,
                    fontSize = if (compact) 14.sp else 12.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MonoFilterChip(
    text: String,
    selected: Boolean,
    compact: Boolean = false,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(if (compact) 10.dp else 8.dp)
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = Modifier
            .height(if (compact) 40.dp else 32.dp)
            .animateContentSize(animationSpec = tween(220))
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            ),
        shape = shape,
        color = Color.White,
        border = if (selected) null else BorderStroke(1.dp, FiniBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (compact) 16.dp else 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = FiniBlack,
                    modifier = Modifier.size(if (compact) 18.dp else 16.dp)
                )
                Spacer(modifier = Modifier.width(if (compact) 10.dp else 7.dp))
            }
            Text(
                text = text,
                color = FiniBlack,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Normal,
                    fontSize = if (compact) 14.sp else 12.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryChip(
    category: CategoryResponse,
    selected: Boolean,
    compact: Boolean = false,
    onClick: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    val color = colorFromHex(category.color)
    var menuExpanded by remember(category.id) { mutableStateOf(false) }
    val shape = RoundedCornerShape(if (compact) 10.dp else 8.dp)
    val interactionSource = remember { MutableInteractionSource() }

    Box {
        Surface(
            modifier = Modifier
                .height(if (compact) 40.dp else 32.dp)
                .animateContentSize(animationSpec = tween(220))
                .clip(shape)
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    onClick = onClick,
                    onLongClick = { menuExpanded = true }
                ),
            shape = shape,
            color = Color.White,
            border = if (selected) null else BorderStroke(1.dp, FiniBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = if (compact) 16.dp else 12.dp),
                horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategorySelectionMark(
                    selected = selected,
                    color = color,
                    isAutomotive = compact
                )
                Text(
                    text = category.name,
                    color = color,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Normal,
                        fontSize = if (compact) 14.sp else 12.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            modifier = Modifier.width(86.dp),
            shape = RoundedCornerShape(10.dp),
            containerColor = Color.White,
            tonalElevation = 0.dp,
            shadowElevation = 8.dp
        ) {
            FilterMenuItem("Xoá", false, showCheck = false, onClick = {
                menuExpanded = false
                onDeleteRequest()
            })
        }
    }
}

@Composable
private fun CategorySelectionMark(
    selected: Boolean,
    color: Color,
    isAutomotive: Boolean = false
) {
    Box(
        modifier = Modifier.size(if (isAutomotive) 20.dp else 16.dp),
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
                    modifier = Modifier.size(if (isAutomotive) 20.dp else 16.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(if (isAutomotive) 12.dp else 9.dp)
                        .background(color, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(
    text: String,
    compact: Boolean,
    isAutomotive: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (isAutomotive) 12.dp else if (compact) 2.dp else 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = FiniBlack,
            style = if (isAutomotive) {
                MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp)
            } else if (compact) {
                MaterialTheme.typography.bodyMedium
            } else {
                MaterialTheme.typography.titleMedium
            },
            fontWeight = FontWeight.Bold
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = FiniBorder
        )
    }
}

@Composable
private fun NoteCard(
    task: TaskResponse,
    categories: List<CategoryResponse>,
    compact: Boolean,
    isAutomotive: Boolean = false,
    trashMode: Boolean,
    onOpen: () -> Unit,
    onTogglePriority: () -> Unit,
    onDelete: () -> Unit
) {
    val category = categories.firstOrNull { it.id == task.categoryId }
    val categoryColor = if (category == null) FiniGray else colorFromHex(category.color)
    val categoryName = category?.name ?: "----------"
    val notificationLine1 = taskNotificationLine1(task)
    val notificationLine2 = taskNotificationLine2(task)

    val priorityInteractionSource = remember { MutableInteractionSource() }
    val priorityIsPressed by priorityInteractionSource.collectIsPressedAsState()
    val priorityScale by animateFloatAsState(
        targetValue = if (priorityIsPressed) 0.85f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "priority-icon-scale"
    )

    val deleteInteractionSource = remember { MutableInteractionSource() }
    val deleteIsPressed by deleteInteractionSource.collectIsPressedAsState()
    val deleteScale by animateFloatAsState(
        targetValue = if (deleteIsPressed) 0.85f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "delete-icon-scale"
    )

    val priorityRipple = ripple(
        bounded = false,
        radius = if (isAutomotive) 20.dp else if (compact) 16.dp else 18.dp
    )
    val deleteRipple = ripple(
        bounded = false,
        radius = if (isAutomotive) 20.dp else if (compact) 16.dp else 18.dp
    )
    val directionsInteractionSource = remember { MutableInteractionSource() }
    val directionsIsPressed by directionsInteractionSource.collectIsPressedAsState()
    val directionsScale by animateFloatAsState(
        targetValue = if (directionsIsPressed) 0.85f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "directions-icon-scale"
    )
    val directionsRipple = ripple(
        bounded = false,
        radius = if (isAutomotive) 20.dp else if (compact) 16.dp else 18.dp
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clickable(onClick = onOpen),
            shape = RoundedCornerShape(if (isAutomotive) 12.dp else 12.dp),
            colors = CardDefaults.cardColors(containerColor = FiniSurface),
            border = BorderStroke(1.dp, FiniBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isAutomotive) 15.dp else if (compact) 11.dp else 13.dp)
            ) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(if (isAutomotive) 6.dp else 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (category != null) {
                            Box(
                                modifier = Modifier
                                    .size(if (isAutomotive) 10.dp else 8.dp)
                                    .background(categoryColor, CircleShape)
                            )
                        }
                        Text(
                            text = categoryName,
                            color = categoryColor,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = if (isAutomotive) 12.sp else if (compact) 9.sp else 10.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(if (isAutomotive) 24.dp else if (compact) 18.dp else 20.dp)
                            .graphicsLayer(scaleX = priorityScale, scaleY = priorityScale)
                            .clickable(
                                role = Role.Button,
                                interactionSource = priorityInteractionSource,
                                indication = priorityRipple,
                                onClick = onTogglePriority
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (task.priority) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = if (task.priority) "Bỏ ưu tiên" else "Đánh dấu ưu tiên",
                            tint = if (task.priority) FiniBlack else FiniGray,
                            modifier = Modifier.size(if (isAutomotive) 20.dp else if (compact) 15.dp else 17.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .padding(
                            top = if (isAutomotive) 26.dp else if (compact) 22.dp else 24.dp,
                            bottom = if (isAutomotive) 42.dp else if (compact) 34.dp else 38.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(if (isAutomotive) 6.dp else if (compact) 4.dp else 5.dp)
                ) {
                    Text(
                        text = task.title,
                        color = FiniBlack,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = if (isAutomotive) 16.sp else if (compact) 11.sp else 13.sp
                        ),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = task.note.orEmpty(),
                        color = FiniBlack,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = if (isAutomotive) 13.sp else if (compact) 9.sp else 11.sp
                        ),
                        maxLines = if (compact) 2 else 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(if (isAutomotive) 4.dp else 1.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(if (isAutomotive) 6.dp else 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = FiniGray,
                                modifier = Modifier.size(if (isAutomotive) 16.dp else if (compact) 11.dp else 13.dp)
                            )
                            Text(
                                text = notificationLine1,
                                color = FiniGray,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = if (isAutomotive) 12.sp else if (compact) 9.sp else 10.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (!trashMode && task.latitude != null && task.longitude != null) {
                            val context = LocalContext.current
                            Box(
                                modifier = Modifier
                                    .padding(start = 6.dp)
                                    .size(if (isAutomotive) 24.dp else if (compact) 18.dp else 20.dp)
                                    .graphicsLayer(scaleX = directionsScale, scaleY = directionsScale)
                                    .clickable(
                                        role = Role.Button,
                                        interactionSource = directionsInteractionSource,
                                        indication = directionsRipple,
                                        onClick = {
                                            runCatching {
                                                val uri = Uri.parse("google.navigation:q=${task.latitude},${task.longitude}")
                                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                            }
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NearMe,
                                    contentDescription = "Chỉ đường",
                                    tint = FiniGray,
                                    modifier = Modifier.size(if (isAutomotive) 20.dp else if (compact) 14.dp else 16.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = notificationLine2,
                            color = FiniGray,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = if (isAutomotive) 12.sp else if (compact) 9.sp else 10.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Box(
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .size(if (isAutomotive) 24.dp else if (compact) 18.dp else 20.dp)
                                .graphicsLayer(scaleX = deleteScale, scaleY = deleteScale)
                                .clickable(
                                    role = Role.Button,
                                    interactionSource = deleteInteractionSource,
                                    indication = deleteRipple,
                                    onClick = onDelete
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (trashMode) Icons.Default.RestoreFromTrash else Icons.Default.Delete,
                                contentDescription = if (trashMode) "Khôi phục" else "Bỏ vào thùng rác",
                                tint = FiniGray,
                                modifier = Modifier.size(if (isAutomotive) 20.dp else if (compact) 14.dp else 16.dp)
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = formatDisplayDate(task.createdAt),
            color = FiniGray,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = if (isAutomotive) 13.sp else 11.sp
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 5.dp),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun isWideCompactHome(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.screenWidthDp >= 700 && configuration.screenHeightDp <= 700
}

private fun buildTaskSections(
    tasks: List<TaskResponse>,
    dateFilter: String
): List<TaskSection> {
    if (tasks.isEmpty()) {
        return emptyList()
    }

    return when (dateFilter) {
        "TODAY" -> buildTodaySections(tasks)
        "THIS_WEEK" -> buildThisWeekSections(tasks)
        else -> buildCreatedDateSections(tasks)
    }
}

private fun buildCreatedDateSections(tasks: List<TaskResponse>): List<TaskSection> {
    return tasks
        .sortedByDescending { createdMillis(it) }
        .groupBy { formatDisplayDate(it.createdAt).ifBlank { "Không rõ ngày tạo" } }
        .map { (title, dayTasks) ->
            TaskSection(
                title = title,
                tasks = dayTasks.sortedByDescending { createdMillis(it) }
            )
        }
}

private fun buildTodaySections(tasks: List<TaskResponse>): List<TaskSection> {
    val today = todayStart()
    return tasks
        .sortedBy { nextNotificationMillis(it, today) }
        .groupBy { task ->
            val notification = nextNotificationAt(task, today)
            formatDisplayTime(notification).ifBlank { "Không rõ giờ" }
        }
        .map { (title, dayTasks) ->
            TaskSection(
                title = title,
                tasks = dayTasks.sortedBy { nextNotificationMillis(it, today) }
            )
        }
}

private fun buildThisWeekSections(tasks: List<TaskResponse>): List<TaskSection> {
    val today = todayStart()
    val endOfWeek = calendarClone(today).apply {
        val daysUntilSunday = (Calendar.SUNDAY - get(Calendar.DAY_OF_WEEK) + 7) % 7
        add(Calendar.DAY_OF_YEAR, daysUntilSunday)
    }

    val grouped = tasks
        .sortedBy { nextNotificationMillis(it, today) }
        .groupBy { task -> nextNotificationAt(task, today)?.let(::dayKey).orEmpty() }

    val sections = mutableListOf<TaskSection>()
    val cursor = calendarClone(today)

    while (!cursor.after(endOfWeek)) {
        val key = dayKey(cursor)
        val dayTasks = grouped[key]
            .orEmpty()
            .sortedBy { nextNotificationMillis(it, today) }
        if (dayTasks.isNotEmpty()) {
            sections += TaskSection(
                title = if (sameDay(cursor, today)) "Hôm nay" else weekdayLabel(cursor),
                tasks = dayTasks
            )
        }
        cursor.add(Calendar.DAY_OF_YEAR, 1)
    }

    return sections.ifEmpty { listOf(TaskSection("Tuần này", tasks)) }
}

private fun formatDisplayDate(value: String?): String {
    val calendar = calendarFromApiValue(value) ?: return ""
    return formatDate(calendar)
}

private fun formatDate(calendar: Calendar): String {
    return SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN")).format(calendar.time)
}

private fun weekdayLabel(calendar: Calendar): String {
    return when (calendar.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> "Thứ 2"
        Calendar.TUESDAY -> "Thứ 3"
        Calendar.WEDNESDAY -> "Thứ 4"
        Calendar.THURSDAY -> "Thứ 5"
        Calendar.FRIDAY -> "Thứ 6"
        Calendar.SATURDAY -> "Thứ 7"
        Calendar.SUNDAY -> "Chủ Nhật"
        else -> "Tuần này"
    }
}

private fun taskNotificationLine1(task: TaskResponse): String {
    if (!hasReminderData(task)) {
        return "--:--"
    }
    val repeatType = task.repeatType.uppercase()
    return if (repeatType == "NONE") {
        calendarFromApiValue(task.notifyAt ?: task.dueAt)?.let(::formatTime24).orEmpty()
            .ifBlank { "--:--" }
    } else {
        task.notifyTime?.let(::compactTime)
            ?: calendarFromApiValue(task.dueAt)?.let(::formatTime24)
            .orEmpty()
            .ifBlank { "--:--" }
    }
}

private fun formatDisplayTime(calendar: Calendar?): String {
    calendar ?: return ""
    return SimpleDateFormat("hh:mm a", Locale.US).format(calendar.time)
}

private fun taskNotificationLine2(task: TaskResponse): String {
    if (!hasReminderData(task)) {
        return "--/--/----"
    }
    val repeatType = task.repeatType.uppercase()
    return when (repeatType) {
        "DAILY" -> "Hằng ngày"
        "WEEKLY" -> weeklyShortLabel(task.repeatDays)
        else -> calendarFromApiValue(task.notifyAt ?: task.dueAt)?.let(::formatDate).orEmpty()
            .ifBlank { "--/--/----" }
    }
}

private fun formatTime24(calendar: Calendar): String {
    return SimpleDateFormat("HH:mm", Locale.US).format(calendar.time)
}

private fun weeklyShortLabel(repeatDays: String?): String {
    val days = repeatDays.orEmpty()
        .split(",", " ")
        .mapNotNull { token ->
            when (token.trim().uppercase()) {
                "1", "MON", "MONDAY" -> "T2"
                "2", "TUE", "TUESDAY" -> "T3"
                "3", "WED", "WEDNESDAY" -> "T4"
                "4", "THU", "THURSDAY" -> "T5"
                "5", "FRI", "FRIDAY" -> "T6"
                "6", "SAT", "SATURDAY" -> "T7"
                "7", "SUN", "SUNDAY" -> "CN"
                else -> null
            }
        }

    return if (days.isEmpty()) "Hằng tuần" else days.joinToString(", ")
}

private fun compactTime(value: String): String {
    val parts = value.split(":")
    if (parts.size < 2) {
        return value
    }
    return "${parts[0].padStart(2, '0')}:${parts[1].padStart(2, '0')}"
}

private fun hasReminderData(task: TaskResponse): Boolean {
    return !task.notifyAt.isNullOrBlank() ||
            !task.dueAt.isNullOrBlank() ||
            !task.notifyTime.isNullOrBlank()
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

private fun parseRepeatDays(value: String?): Set<Int> {
    return value.orEmpty()
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

private fun dayKey(calendar: Calendar): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
}

private fun sameDay(left: Calendar, right: Calendar): Boolean {
    return left.get(Calendar.YEAR) == right.get(Calendar.YEAR) &&
            left.get(Calendar.DAY_OF_YEAR) == right.get(Calendar.DAY_OF_YEAR)
}

private fun todayStart(): Calendar {
    return Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}

private fun calendarFromApiValue(value: String?): Calendar? {
    if (value.isNullOrBlank()) {
        return null
    }
    return runCatching {
        Calendar.getInstance().apply {
            time = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(value)!!
        }
    }.getOrNull()
}

private fun calendarClone(calendar: Calendar): Calendar {
    return calendar.clone() as Calendar
}

private fun createdMillis(task: TaskResponse): Long {
    return calendarFromApiValue(task.createdAt)?.timeInMillis ?: Long.MIN_VALUE
}

private fun nextNotificationMillis(task: TaskResponse, fromInclusive: Calendar): Long {
    return nextNotificationAt(task, fromInclusive)?.timeInMillis ?: Long.MAX_VALUE
}

@Composable
private fun EmptyNotes(
    isTrash: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isTrash) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = Color(0xFF999999),
                modifier = Modifier.size(72.dp)
            )
        } else {
            Surface(
                color = FiniLightGray,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = FiniBlack,
                    modifier = Modifier
                        .padding(24.dp)
                        .size(36.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isTrash) "Thùng rác trống" else "Chưa có ghi chú",
            style = MaterialTheme.typography.titleMedium,
            color = FiniBlack
        )
        if (!isTrash) {
            Text(
                text = "Bấm dấu cộng để tạo ghi chú đầu tiên.",
                style = MaterialTheme.typography.bodyMedium,
                color = FiniGray
            )
        }
    }
}

@Composable
private fun ErrorBanner(text: String) {
    Surface(
        color = FiniLightGray,
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            text = text,
            color = FiniBlack,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
private fun CreateCategoryDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#111111") }
    val colors = listOf(
        "#111111", "#777777", "#D92D20", "#FF7A00",
        "#FDB022", "#12B76A", "#06AED4", "#0066CC",
        "#7A5AF8", "#DD2590", "#A15C38", "#344054"
    )

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val animateTrigger = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animateTrigger.value = true
    }
    val scale by animateFloatAsState(
        targetValue = if (animateTrigger.value) 1f else 0.92f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "create-dialog-scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (animateTrigger.value) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "create-dialog-alpha"
    )

    val cancelInteractionSource = remember { MutableInteractionSource() }
    val cancelIsPressed by cancelInteractionSource.collectIsPressedAsState()
    val cancelScale by animateFloatAsState(
        targetValue = if (cancelIsPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "cancel-scale"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .width(356.dp)
                .graphicsLayer(scaleX = scale, scaleY = scale, alpha = alpha)
                .pointerInput(Unit) {
                    detectTapGestures {
                        focusManager.clearFocus()
                    }
                },
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            shadowElevation = 18.dp
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Tạo category",
                    color = FiniBlack,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên category") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedTextColor = FiniBlack,
                        unfocusedTextColor = FiniBlack,
                        cursorColor = FiniBlack,
                        focusedBorderColor = FiniBorder,
                        unfocusedBorderColor = FiniBorder,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                Text(
                    text = "Chọn màu",
                    style = MaterialTheme.typography.labelLarge,
                    color = FiniBlack
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    colors.chunked(6).forEach { row ->
                         Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            row.forEach { value ->
                                val color = colorFromHex(value)
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(color, CircleShape)
                                            .border(
                                                width = 1.dp,
                                                color = FiniBorder,
                                                shape = CircleShape
                                            )
                                            .clip(CircleShape)
                                            .clickable(
                                                role = Role.Button
                                            ) {
                                                selectedColor = value
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (selectedColor == value) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onDismiss,
                        modifier = Modifier
                            .width(92.dp)
                            .height(38.dp)
                            .graphicsLayer(scaleX = cancelScale, scaleY = cancelScale),
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, FiniBlack),
                        interactionSource = cancelInteractionSource
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Hủy",
                                color = FiniBlack,
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    MonoActionButton(
                        text = "Tạo",
                        modifier = Modifier.width(92.dp),
                        fillMaxWidth = false,
                        onClick = {
                            if (name.isNotBlank()) {
                                onCreate(name.trim(), selectedColor)
                            } else {
                                android.widget.Toast.makeText(
                                    context,
                                    "Vui lòng nhập tên Category",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        enabled = true,
                        height = 38.dp
                    )
                }
            }
        }
    }
}

private fun colorFromHex(hex: String?): Color {
    return runCatching {
        Color(android.graphics.Color.parseColor(hex ?: "#111111"))
    }.getOrDefault(FiniBlack)
}
