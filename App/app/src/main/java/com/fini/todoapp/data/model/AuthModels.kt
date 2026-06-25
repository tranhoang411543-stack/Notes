package com.fini.todoapp.data.model

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

data class LoginRequest(
    val emailOrUsername: String,
    val password: String
)

data class ForgotPasswordRequest(
    val email: String
)

data class ResetPasswordRequest(
    val email: String,
    val otp: String,
    val newPassword: String
)

data class AuthResponse(
    val accessToken: String,
    val tokenType: String,
    val userId: String,
    val username: String,
    val email: String
)

data class MessageResponse(
    val message: String
)
