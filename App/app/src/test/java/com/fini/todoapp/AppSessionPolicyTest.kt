package com.fini.todoapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.fini.todoapp.ui.auth.AuthCopy

class AppSessionPolicyTest {

    @Test
    fun startScreenUsesHomeWhenSessionOrOfflineModeExists() {
        assertEquals(AppScreen.LOGIN, AppSessionPolicy.startScreen(hasSession = false, offlineMode = false))
        assertEquals(AppScreen.HOME, AppSessionPolicy.startScreen(hasSession = false, offlineMode = true))
        assertEquals(AppScreen.HOME, AppSessionPolicy.startScreen(hasSession = true, offlineMode = false))
        assertEquals(AppScreen.HOME, AppSessionPolicy.startScreen(hasSession = true, offlineMode = true))
    }

    @Test
    fun continuingOfflineOpensHomeWithoutSession() {
        assertEquals(AppScreen.HOME, AppSessionPolicy.continueOfflineScreen(hasPendingNotification = false))
    }

    @Test
    fun continuingOfflineCanOpenPendingNotification() {
        assertEquals(AppScreen.TASK_DETAIL, AppSessionPolicy.continueOfflineScreen(hasPendingNotification = true))
    }

    @Test
    fun syncRequiresNetworkAndSession() {
        assertFalse(AppSessionPolicy.shouldRunSync(hasSession = false, hasNetwork = false))
        assertFalse(AppSessionPolicy.shouldRunSync(hasSession = false, hasNetwork = true))
        assertFalse(AppSessionPolicy.shouldRunSync(hasSession = true, hasNetwork = false))
        assertTrue(AppSessionPolicy.shouldRunSync(hasSession = true, hasNetwork = true))
    }

    @Test
    fun localChangesEnqueueSyncAutomaticallyWhenSessionExists() {
        assertFalse(AppSessionPolicy.shouldSyncAfterLocalChange(hasSession = false))
        assertTrue(AppSessionPolicy.shouldSyncAfterLocalChange(hasSession = true))
    }

    @Test
    fun manualSyncWithoutSessionRequiresLoginScreen() {
        assertEquals(AppScreen.LOGIN, AppSessionPolicy.manualSyncScreen(hasSession = false))
        assertEquals(AppScreen.HOME, AppSessionPolicy.manualSyncScreen(hasSession = true))
    }

    @Test
    fun loginScreenUsesExplicitOfflineContinueLabel() {
        assertEquals("Tiếp tục mà không cần đăng nhập", AuthCopy.CONTINUE_WITHOUT_LOGIN)
    }

    @Test
    fun loginScreenUsesSyncSubtitle() {
        assertEquals("Đăng nhập để đồng bộ", AuthCopy.LOGIN_SUBTITLE)
    }

    @Test
    fun authFlowUsesRequestedToastMessages() {
        assertEquals("Bạn cần đăng nhập để đồng bộ", AuthCopy.LOGIN_REQUIRED_TO_SYNC)
        assertEquals("Bạn đã đăng xuất", AuthCopy.LOGGED_OUT)
    }
}
