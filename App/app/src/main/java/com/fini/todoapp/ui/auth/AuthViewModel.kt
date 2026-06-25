package com.fini.todoapp.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fini.todoapp.data.AuthTokenStore
import com.fini.todoapp.data.ErrorUtils
import com.fini.todoapp.data.api.ApiClient
import com.fini.todoapp.data.model.ForgotPasswordRequest
import com.fini.todoapp.data.model.LoginRequest
import com.fini.todoapp.data.model.RegisterRequest
import com.fini.todoapp.data.model.ResetPasswordRequest
import kotlinx.coroutines.launch
import java.net.SocketTimeoutException

data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

class AuthViewModel : ViewModel() {

    var state by mutableStateOf(AuthUiState())
        private set

    fun login(
        emailOrUsername: String,
        password: String,
        onSuccess: () -> Unit
    ) {
        state = state.copy(loading = true, error = null, message = null)

        viewModelScope.launch {
            try {
                val response = ApiClient.authApi.login(
                    LoginRequest(
                        emailOrUsername = emailOrUsername,
                        password = password
                    )
                )

                AuthTokenStore.accessToken = response.accessToken
                state = state.copy(loading = false, message = null)
                onSuccess()
            } catch (e: Exception) {
                state = state.copy(
                    loading = false,
                    error = ErrorUtils.getMessage(e),
                    message = null
                )
            }
        }
    }

    fun register(
        username: String,
        email: String,
        password: String,
        onSuccess: () -> Unit
    ) {
        state = state.copy(loading = true, error = null, message = null)

        viewModelScope.launch {
            try {
                ApiClient.authApi.register(
                    RegisterRequest(
                        username = username,
                        email = email,
                        password = password
                    )
                )

                state = state.copy(
                    loading = false,
                    message = "Đăng ký thành công"
                )
                onSuccess()
            } catch (e: Exception) {
                state = state.copy(
                    loading = false,
                    error = ErrorUtils.getMessage(e),
                    message = null
                )
            }
        }
    }

    fun forgotPassword(
        email: String,
        onSuccess: () -> Unit
    ) {
        state = state.copy(loading = true, error = null, message = null)

        viewModelScope.launch {
            try {
                ApiClient.authApi.forgotPassword(
                    ForgotPasswordRequest(email = email)
                )

                state = state.copy(
                    loading = false,
                    message = "OTP đã được gửi tới email"
                )
                onSuccess()
            } catch (e: Exception) {
                if (e.isTimeout()) {
                    state = state.copy(
                        loading = false,
                        message = "Nếu bạn đã nhận OTP, hãy nhập mã bên dưới",
                        error = null
                    )
                    onSuccess()
                } else {
                    state = state.copy(
                        loading = false,
                        error = ErrorUtils.getMessage(e),
                        message = null
                    )
                }
            }
        }
    }

    fun resetPassword(
        email: String,
        otp: String,
        newPassword: String,
        onSuccess: () -> Unit
    ) {
        state = state.copy(loading = true, error = null, message = null)

        viewModelScope.launch {
            try {
                ApiClient.authApi.resetPassword(
                    ResetPasswordRequest(
                        email = email,
                        otp = otp,
                        newPassword = newPassword
                    )
                )

                state = state.copy(
                    loading = false,
                    message = "Đổi mật khẩu thành công"
                )
                onSuccess()
            } catch (e: Exception) {
                state = state.copy(
                    loading = false,
                    error = ErrorUtils.getMessage(e),
                    message = null
                )
            }
        }
    }

    fun clearError() {
        state = state.copy(error = null)
    }

    fun clearFeedback() {
        state = state.copy(error = null, message = null)
    }

    private fun Throwable.isTimeout(): Boolean {
        return this is SocketTimeoutException ||
                message?.contains("timeout", ignoreCase = true) == true ||
                message?.contains("timed out", ignoreCase = true) == true
    }
}
