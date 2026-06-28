package com.fini.todoapp

enum class AppScreen(val route: String) {
    LOGIN("login"),
    REGISTER("register"),
    FORGOT_PASSWORD("forgotPassword"),
    HOME("home"),
    CREATE_TASK("createTask"),
    TASK_DETAIL("taskDetail")
}

object AppSessionPolicy {
    fun startScreen(hasSession: Boolean, offlineMode: Boolean): AppScreen {
        return if (hasSession || offlineMode) AppScreen.HOME else AppScreen.LOGIN
    }

    fun continueOfflineScreen(hasPendingNotification: Boolean): AppScreen {
        return if (hasPendingNotification) AppScreen.TASK_DETAIL else AppScreen.HOME
    }

    fun manualSyncScreen(hasSession: Boolean): AppScreen {
        return if (hasSession) AppScreen.HOME else AppScreen.LOGIN
    }

    fun shouldRunSync(hasSession: Boolean, hasNetwork: Boolean): Boolean {
        return hasSession && hasNetwork
    }

    fun shouldSyncAfterLocalChange(hasSession: Boolean): Boolean {
        return hasSession
    }
}
