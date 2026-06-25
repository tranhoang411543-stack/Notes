package com.fini.todoapp.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object AuthTokenStore {

    private const val PREF_NAME = "note_auth_session"
    private const val KEY_ACCESS_TOKEN = "access_token"

    private var preferences: SharedPreferences? = null
    
    private val _sessionEvent = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val sessionEvent: SharedFlow<Boolean> = _sessionEvent.asSharedFlow()

    var accessToken: String? = null
        set(value) {
            field = value
            preferences?.edit()?.apply {
                if (value.isNullOrBlank()) {
                    remove(KEY_ACCESS_TOKEN)
                } else {
                    putString(KEY_ACCESS_TOKEN, value)
                }
            }?.apply()
            _sessionEvent.tryEmit(!value.isNullOrBlank())
        }

    fun init(context: Context) {
        preferences = context.applicationContext.getSharedPreferences(
            PREF_NAME,
            Context.MODE_PRIVATE
        )
        accessToken = preferences?.getString(KEY_ACCESS_TOKEN, null)
    }

    fun hasSession(): Boolean = !accessToken.isNullOrBlank()

    fun clear() {
        accessToken = null
    }
}
