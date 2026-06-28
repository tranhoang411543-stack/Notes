package com.fini.todoapp.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object AuthTokenStore {

    private const val PREF_NAME = "note_auth_session"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_OFFLINE_MODE = "offline_mode"

    private var preferences: SharedPreferences? = null
    
    private val _sessionEvent = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val sessionEvent: SharedFlow<Boolean> = _sessionEvent.asSharedFlow()

    private var tokenValue: String? = null

    var accessToken: String?
        get() = tokenValue
        set(value) {
            setAccessToken(value, emitEvent = true)
        }

    var offlineMode: Boolean = false
        private set(value) {
            field = value
            preferences?.edit()
                ?.putBoolean(KEY_OFFLINE_MODE, value)
                ?.apply()
        }

    fun init(context: Context) {
        preferences = context.applicationContext.getSharedPreferences(
            PREF_NAME,
            Context.MODE_PRIVATE
        )
        offlineMode = preferences?.getBoolean(KEY_OFFLINE_MODE, false) == true
        setAccessToken(preferences?.getString(KEY_ACCESS_TOKEN, null), emitEvent = false)
    }

    fun hasSession(): Boolean = !accessToken.isNullOrBlank()

    fun hasOfflineMode(): Boolean = offlineMode

    fun hasAppAccess(): Boolean = hasSession() || hasOfflineMode()

    fun continueOffline() {
        setAccessToken(null, emitEvent = false)
        offlineMode = true
    }

    fun clear() {
        accessToken = null
        offlineMode = false
    }

    private fun setAccessToken(value: String?, emitEvent: Boolean) {
        tokenValue = value
        preferences?.edit()?.apply {
            if (value.isNullOrBlank()) {
                remove(KEY_ACCESS_TOKEN)
            } else {
                putString(KEY_ACCESS_TOKEN, value)
                putBoolean(KEY_OFFLINE_MODE, false)
            }
        }?.apply()
        if (!value.isNullOrBlank()) {
            offlineMode = false
        }
        if (emitEvent) {
            _sessionEvent.tryEmit(!value.isNullOrBlank())
        }
    }
}
