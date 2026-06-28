package com.fini.todoapp.ui.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
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
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onContinueOffline: () -> Unit,
    onGoToRegister: () -> Unit,
    onGoToForgotPassword: () -> Unit
) {
    val state = viewModel.state

    var emailOrUsername by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val canLogin = emailOrUsername.isNotBlank() && password.isNotBlank() && !state.loading
    val feedbackMessage = state.error ?: state.message
    val isCar = isAutomotive()
    val submitLogin = {
        if (canLogin) {
            viewModel.login(
                emailOrUsername = emailOrUsername.trim(),
                password = password,
                onSuccess = onLoginSuccess
            )
        }
    }

    LaunchedEffect(feedbackMessage) {
        if (!feedbackMessage.isNullOrBlank()) {
            delay(3800)
            viewModel.clearFeedback()
        }
    }

    AuthScaffold(
        subtitle = AuthCopy.LOGIN_SUBTITLE,
        toastMessage = feedbackMessage
    ) {
        AuthTextField(
            value = emailOrUsername,
            onValueChange = { emailOrUsername = it },
            placeholder = "Email hoặc username",
            leadingIcon = Icons.Default.Person,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            onImeAction = submitLogin
        )

        AuthTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = "Mật khẩu",
            leadingIcon = Icons.Default.Lock,
            imeAction = ImeAction.Done,
            onImeAction = submitLogin,
            password = true
        )

        Spacer(modifier = Modifier.height(if (isCar) 0.dp else 4.dp))

        AuthPrimaryButton(
            text = "Đăng nhập",
            loadingText = "Đang đăng nhập...",
            loading = state.loading,
            enabled = emailOrUsername.isNotBlank() && password.isNotBlank(),
            onClick = submitLogin
        )

        AuthInlineButton(
            text = AuthCopy.CONTINUE_WITHOUT_LOGIN,
            modifier = Modifier.heightIn(min = if (isCar) 30.dp else 0.dp),
            onClick = {
                viewModel.clearFeedback()
                onContinueOffline()
            }
        )

        AuthInlineButton(
            text = "Quên mật khẩu?",
            modifier = Modifier.heightIn(min = if (isCar) 30.dp else 0.dp),
            onClick = {
                viewModel.clearFeedback()
                onGoToForgotPassword()
            }
        )

        AuthPromptLink(
            prefix = "Chưa có tài khoản?",
            linkText = "Đăng ký",
            onClick = {
                viewModel.clearFeedback()
                onGoToRegister()
            }
        )
    }
}
