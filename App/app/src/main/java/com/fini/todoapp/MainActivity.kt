package com.fini.todoapp

import android.Manifest
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.content.Intent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fini.todoapp.data.AuthTokenStore
import com.fini.todoapp.notification.TaskNotificationScheduler
import com.fini.todoapp.ui.auth.AuthViewModel
import com.fini.todoapp.ui.auth.ForgotPasswordScreen
import com.fini.todoapp.ui.auth.LoginScreen
import com.fini.todoapp.ui.auth.RegisterScreen
import com.fini.todoapp.ui.home.CreateTaskScreen
import com.fini.todoapp.ui.home.HomeScreen
import com.fini.todoapp.ui.home.HomeViewModel
import com.fini.todoapp.ui.theme.FiniTodoTheme

class MainActivity : ComponentActivity() {

    private val notificationTaskIdState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AuthTokenStore.init(applicationContext)
        TaskNotificationScheduler.ensureChannel(applicationContext)
        requestNotificationPermissionIfNeeded()
        configureSystemBars()

        handleIntent(intent)

        setContent {
            FiniTodoTheme {
                FiniApp(
                    notificationTaskId = notificationTaskIdState.value,
                    onNotificationConsumed = {
                        notificationTaskIdState.value = null
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val taskId = intent?.getStringExtra("task_id")
        if (taskId != null) {
            notificationTaskIdState.value = taskId
        }
    }

    private fun configureSystemBars() {
        val appBackground = Color.parseColor("#F5F5F5")
        window.statusBarColor = appBackground
        window.navigationBarColor = appBackground

        var flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
        window.decorView.systemUiVisibility = flags
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }

        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 411)
        }
    }
}

@Composable
fun FiniApp(notificationTaskId: String? = null, onNotificationConsumed: () -> Unit = {}) {
    var screen by remember {
        mutableStateOf(if (AuthTokenStore.hasSession()) "home" else "login")
    }
    var selectedTaskId by remember { mutableStateOf<String?>(null) }
    var pendingNotificationTaskId by remember { mutableStateOf<String?>(null) }

    val authViewModel: AuthViewModel = viewModel()
    val homeViewModel: HomeViewModel = viewModel()

    val context = LocalContext.current
    val isAutomotive = remember {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE) ||
        (context.getSystemService(android.content.Context.UI_MODE_SERVICE) as? android.app.UiModeManager)?.currentModeType == android.content.res.Configuration.UI_MODE_TYPE_CAR ||
        android.os.Build.DEVICE.contains("car", ignoreCase = true) ||
        android.os.Build.MODEL.contains("car", ignoreCase = true) ||
        android.os.Build.FINGERPRINT.contains("car", ignoreCase = true) ||
        (context.resources.configuration.screenWidthDp >= 800 && context.resources.configuration.screenWidthDp > context.resources.configuration.screenHeightDp)
    }

    LaunchedEffect(Unit) {
        AuthTokenStore.sessionEvent.collect { hasSession ->
            if (!hasSession) {
                selectedTaskId = null
                homeViewModel.clearLocalData()
                homeViewModel.resetSession()
                screen = "login"
            }
        }
    }

    LaunchedEffect(notificationTaskId) {
        if (notificationTaskId != null) {
            if (AuthTokenStore.hasSession()) {
                selectedTaskId = notificationTaskId
                screen = "taskDetail"
                onNotificationConsumed()
            } else {
                pendingNotificationTaskId = notificationTaskId
            }
        }
    }

    AnimatedContent(
        targetState = screen,
        label = "screen-transition",
        transitionSpec = {
            if (isAutomotive) {
                (slideInVertically { height -> -height } + fadeIn())
                    .togetherWith(slideOutVertically { height -> height } + fadeOut())
            } else {
                (slideInHorizontally { width -> width } + fadeIn())
                    .togetherWith(slideOutHorizontally { width -> -width } + fadeOut())
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { targetScreen ->
        when (targetScreen) {
            "login" -> {
                LoginScreen(
                    viewModel = authViewModel,
                    onLoginSuccess = {
                        homeViewModel.resetSession()
                        homeViewModel.loadAll()
                        if (pendingNotificationTaskId != null) {
                            selectedTaskId = pendingNotificationTaskId
                            screen = "taskDetail"
                            pendingNotificationTaskId = null
                            onNotificationConsumed()
                        } else {
                            screen = "home"
                        }
                    },
                    onGoToRegister = {
                        screen = "register"
                    },
                    onGoToForgotPassword = {
                        screen = "forgotPassword"
                    }
                )
            }

            "register" -> {
                RegisterScreen(
                    viewModel = authViewModel,
                    onRegisterSuccess = {
                        screen = "login"
                    },
                    onBackToLogin = {
                        screen = "login"
                    }
                )
            }

            "forgotPassword" -> {
                ForgotPasswordScreen(
                    viewModel = authViewModel,
                    onBackToLogin = {
                        screen = "login"
                    }
                )
            }

            "home" -> {
                HomeScreen(
                    viewModel = homeViewModel,
                    onCreateTask = {
                        screen = "createTask"
                    },
                    onOpenTask = { taskId ->
                        selectedTaskId = taskId
                        screen = "taskDetail"
                    },
                    onLogout = {
                        selectedTaskId = null
                        homeViewModel.clearLocalData()
                        homeViewModel.resetSession()
                        screen = "login"
                    }
                )
            }

            "createTask" -> {
                CreateTaskScreen(
                    categories = homeViewModel.state.categories,
                    onBack = {
                        homeViewModel.loadAll()
                        screen = "home"
                    },
                    onCreate = { request ->
                        homeViewModel.createTask(request)
                    }
                )
            }

            "taskDetail" -> {
                val task = selectedTaskId?.let { taskId ->
                    homeViewModel.state.allTasks.firstOrNull { it.id == taskId } ?:
                    homeViewModel.state.tasks.firstOrNull { it.id == taskId }
                }

                if (task == null) {
                    LaunchedEffect(selectedTaskId) {
                        homeViewModel.loadAll()
                        screen = "home"
                    }
                } else {
                    CreateTaskScreen(
                        categories = homeViewModel.state.categories,
                        task = task,
                        onBack = {
                            selectedTaskId = null
                            homeViewModel.loadAll()
                            screen = "home"
                        },
                        onCreate = { request ->
                            homeViewModel.createTask(request)
                        },
                        onUpdate = { taskId, request ->
                            homeViewModel.updateTask(taskId, request)
                        }
                    )
                }
            }
        }
    }
}
