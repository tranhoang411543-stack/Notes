package com.fini.todoapp.ui.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun ForgotPasswordScreen(
    viewModel: AuthViewModel,
    onBackToLogin: () -> Unit
) {
    val state = viewModel.state

    var email by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    var resetComplete by remember { mutableStateOf(false) }
    val canSendOtp = email.isNotBlank() && !state.loading && !resetComplete
    val requestOtp = {
        if (canSendOtp) {
            localError = null
            otp = ""
            newPassword = ""
            viewModel.forgotPassword(
                email = email.trim(),
                onSuccess = { otpSent = true }
            )
        }
    }
    val canReset = otpSent &&
            otp.length == 6 &&
            newPassword.length >= 6 &&
            !state.loading &&
            !resetComplete
    val submitReset = {
        localError = null
        when {
            !otpSent -> requestOtp()
            otp.length != 6 -> localError = "OTP phải gồm đúng 6 chữ số"
            newPassword.length < 6 -> localError = "Mật khẩu mới phải có ít nhất 6 ký tự"
            canReset -> {
                viewModel.resetPassword(
                    email = email.trim(),
                    otp = otp,
                    newPassword = newPassword,
                    onSuccess = {
                        resetComplete = true
                    }
                )
            }
        }
    }

    LaunchedEffect(resetComplete) {
        if (resetComplete) {
            delay(1100)
            viewModel.clearFeedback()
            onBackToLogin()
        }
    }

    AuthScaffold(
        subtitle = "Nhập email để nhận mã OTP",
        toastMessage = localError ?: state.error ?: state.message
    ) {
        AuthTextField(
            value = email,
            onValueChange = {
                email = it
                localError = null
            },
            placeholder = "Email",
            leadingIcon = Icons.Default.Email,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Send,
            onImeAction = requestOtp,
            enabled = !state.loading && !resetComplete
        )

        AuthPrimaryButton(
            text = if (otpSent) "Gửi lại OTP" else "Gửi OTP",
            loadingText = "Đang gửi...",
            loading = state.loading && !resetComplete,
            enabled = email.isNotBlank() && !resetComplete,
            onClick = requestOtp
        )

        Spacer(modifier = Modifier.height(if (isAutomotive()) 0.dp else 4.dp))

        AuthOtpField(
            value = otp,
            onValueChange = {
                otp = it
                localError = null
            },
            enabled = otpSent && !state.loading && !resetComplete,
            imeAction = ImeAction.Next,
            onImeAction = submitReset
        )

        AuthTextField(
            value = newPassword,
            onValueChange = {
                newPassword = it
                localError = null
            },
            placeholder = "Mật khẩu mới",
            leadingIcon = Icons.Default.Lock,
            imeAction = ImeAction.Done,
            onImeAction = submitReset,
            password = true,
            enabled = otpSent && !state.loading && !resetComplete
        )

        AuthPrimaryButton(
            text = "Đặt lại mật khẩu",
            loadingText = "Đang đặt lại...",
            loading = state.loading && otpSent,
            enabled = otpSent && otp.length == 6 && newPassword.length >= 6 && !resetComplete,
            onClick = submitReset
        )

        AuthInlineButton(
            text = "Quay lại đăng nhập",
            onClick = {
                viewModel.clearFeedback()
                onBackToLogin()
            }
        )
    }
}
