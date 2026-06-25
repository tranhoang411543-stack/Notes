package com.fini.todoapp.data.api

import com.fini.todoapp.data.model.AuthResponse
import com.fini.todoapp.data.model.ForgotPasswordRequest
import com.fini.todoapp.data.model.LoginRequest
import com.fini.todoapp.data.model.MessageResponse
import com.fini.todoapp.data.model.RegisterRequest
import com.fini.todoapp.data.model.ResetPasswordRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("api/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): AuthResponse

    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): AuthResponse

    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(
        @Body request: ForgotPasswordRequest
    ): MessageResponse

    @POST("api/auth/reset-password")
    suspend fun resetPassword(
        @Body request: ResetPasswordRequest
    ): MessageResponse
}
