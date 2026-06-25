package com.fini.todoapp.ui.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
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
fun RegisterScreen(
    viewModel: AuthViewModel,
    onRegisterSuccess: () -> Unit,
    onBackToLogin: () -> Unit
) {
    val state = viewModel.state

    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    val canRegister = username.isNotBlank() &&
            email.isNotBlank() &&
            password.length >= 6 &&
            confirmPassword == password &&
            !state.loading
    val submitRegister = {
        when {
            password.length < 6 -> localError = "Mật khẩu phải có ít nhất 6 ký tự"
            confirmPassword != password -> localError = "Mật khẩu xác nhận không khớp"
            canRegister -> {
                localError = null
                viewModel.register(
                    username = username.trim(),
                    email = email.trim(),
                    password = password,
                    onSuccess = onRegisterSuccess
                )
            }
        }
    }

    LaunchedEffect(localError, state.error) {
        if (!localError.isNullOrBlank() || !state.error.isNullOrBlank()) {
            delay(3600)
            if (localError != null) {
                localError = null
            }
            if (state.error != null) {
                viewModel.clearFeedback()
            }
        }
    }

    AuthScaffold(
        subtitle = "Tạo tài khoản mới",
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
            imeAction = ImeAction.Next,
            onImeAction = submitRegister
        )

        AuthTextField(
            value = username,
            onValueChange = {
                username = it
                localError = null
            },
            placeholder = "Username",
            leadingIcon = Icons.Default.Person,
            imeAction = ImeAction.Next,
            onImeAction = submitRegister
        )

        AuthTextField(
            value = password,
            onValueChange = {
                password = it
                localError = null
            },
            placeholder = "Mật khẩu",
            leadingIcon = Icons.Default.Lock,
            imeAction = ImeAction.Next,
            onImeAction = submitRegister,
            password = true
        )

        AuthTextField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                localError = null
            },
            placeholder = "Xác nhận mật khẩu",
            leadingIcon = Icons.Default.Lock,
            imeAction = ImeAction.Done,
            onImeAction = submitRegister,
            password = true
        )

        Spacer(modifier = Modifier.height(if (isAutomotive()) 0.dp else 4.dp))

        AuthPrimaryButton(
            text = "Đăng ký",
            loadingText = "Đang đăng ký...",
            loading = state.loading,
            enabled = username.isNotBlank() &&
                    email.isNotBlank() &&
                    password.length >= 6 &&
                    confirmPassword == password,
            onClick = submitRegister
        )

        Spacer(modifier = Modifier.height(if (isAutomotive()) 0.dp else 8.dp))

        AuthPromptLink(
            prefix = "Đã có tài khoản?",
            linkText = "Đăng nhập",
            onClick = {
                viewModel.clearFeedback()
                localError = null
                onBackToLogin()
            }
        )
    }
}
