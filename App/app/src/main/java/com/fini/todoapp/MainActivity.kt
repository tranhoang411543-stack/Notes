package com.fini.todoapp

import android.Manifest
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
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
import com.fini.todoapp.ui.auth.AuthCopy
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
        mutableStateOf(
            AppSessionPolicy.startScreen(
                hasSession = AuthTokenStore.hasSession(),
                offlineMode = AuthTokenStore.hasOfflineMode()
            ).route
        )
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
            if (!hasSession && !AuthTokenStore.hasAppAccess()) {
                selectedTaskId = null
                homeViewModel.resetSession()
                screen = AppScreen.LOGIN.route
            }
        }
    }

    LaunchedEffect(notificationTaskId) {
        if (notificationTaskId != null) {
            val isOnAuthScreen = screen == AppScreen.LOGIN.route ||
                    screen == AppScreen.REGISTER.route ||
                    screen == AppScreen.FORGOT_PASSWORD.route
            if (AuthTokenStore.hasSession() || !isOnAuthScreen) {
                selectedTaskId = notificationTaskId
                screen = AppScreen.TASK_DETAIL.route
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
                            screen = AppScreen.TASK_DETAIL.route
                            pendingNotificationTaskId = null
                            onNotificationConsumed()
                        } else {
                            screen = AppScreen.HOME.route
                        }
                    },
                    onContinueOffline = {
                        AuthTokenStore.continueOffline()
                        homeViewModel.resetSession()
                        homeViewModel.loadAll()
                        val destination = AppSessionPolicy.continueOfflineScreen(
                            hasPendingNotification = pendingNotificationTaskId != null
                        )
                        if (destination == AppScreen.TASK_DETAIL) {
                            selectedTaskId = pendingNotificationTaskId
                            pendingNotificationTaskId = null
                            onNotificationConsumed()
                        }
                        screen = destination.route
                    },
                    onGoToRegister = {
                        screen = AppScreen.REGISTER.route
                    },
                    onGoToForgotPassword = {
                        screen = AppScreen.FORGOT_PASSWORD.route
                    }
                )
            }

            "register" -> {
                RegisterScreen(
                    viewModel = authViewModel,
                    onRegisterSuccess = {
                        screen = AppScreen.LOGIN.route
                    },
                    onBackToLogin = {
                        screen = AppScreen.LOGIN.route
                    }
                )
            }

            "forgotPassword" -> {
                ForgotPasswordScreen(
                    viewModel = authViewModel,
                    onBackToLogin = {
                        screen = AppScreen.LOGIN.route
                    }
                )
            }

            "home" -> {
                HomeScreen(
                    viewModel = homeViewModel,
                    onCreateTask = {
                        screen = AppScreen.CREATE_TASK.route
                    },
                    onOpenTask = { taskId ->
                        selectedTaskId = taskId
                        screen = AppScreen.TASK_DETAIL.route
                    },
                    onRefresh = {
                        val destination = AppSessionPolicy.manualSyncScreen(
                            hasSession = AuthTokenStore.hasSession()
                        )
                        if (destination == AppScreen.LOGIN) {
                            AuthTokenStore.clear()
                            selectedTaskId = null
                            homeViewModel.resetSession()
                            Toast.makeText(
                                context,
                                AuthCopy.LOGIN_REQUIRED_TO_SYNC,
                                Toast.LENGTH_SHORT
                            ).show()
                            screen = AppScreen.LOGIN.route
                        } else {
                            homeViewModel.loadAll(syncWithServer = true)
                        }
                    },
                    onLogout = {
                        AuthTokenStore.clear()
                        selectedTaskId = null
                        homeViewModel.resetSession()
                        Toast.makeText(
                            context,
                            AuthCopy.LOGGED_OUT,
                            Toast.LENGTH_SHORT
                        ).show()
                        screen = AppScreen.LOGIN.route
                    }
                )
            }

            "createTask" -> {
                CreateTaskScreen(
                    categories = homeViewModel.state.categories,
                    onBack = {
                        homeViewModel.loadAll()
                        screen = AppScreen.HOME.route
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
                        screen = AppScreen.HOME.route
                    }
                } else {
                    CreateTaskScreen(
                        categories = homeViewModel.state.categories,
                        task = task,
                        onBack = {
                            selectedTaskId = null
                            homeViewModel.loadAll()
                            screen = AppScreen.HOME.route
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
