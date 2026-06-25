package com.fini.todoapp.data

import org.json.JSONObject
import retrofit2.HttpException

object ErrorUtils {
    fun getMessage(e: Throwable): String {
        val message = if (e is HttpException) {
            val body = e.response()?.errorBody()?.string()
            parseMessage(body) ?: body ?: "HTTP ${e.code()}"
        } else {
            e.message ?: "Unknown error"
        }

        return localize(message)
    }

    private fun parseMessage(body: String?): String? {
        if (body.isNullOrBlank()) return null

        return runCatching {
            val json = JSONObject(body)
            json.optString("message")
                .ifBlank { json.optString("error") }
                .ifBlank { null }
        }.getOrNull()
    }

    private fun localize(message: String): String {
        return when {
            message.contains("Invalid OTP", ignoreCase = true) -> {
                "OTP không hợp lệ"
            }

            message.contains("OTP has expired", ignoreCase = true) -> {
                "OTP đã hết hạn"
            }

            message.contains(
                "New password must be different from current password",
                ignoreCase = true
            ) -> {
                "Mật khẩu mới phải khác mật khẩu hiện tại"
            }

            message.contains("Email is invalid", ignoreCase = true) -> {
                "Email không hợp lệ"
            }

            message.contains("Failed to connect", ignoreCase = true) ||
                    message.contains("Connection refused", ignoreCase = true) -> {
                "Không kết nối được backend"
            }

            message.contains("timeout", ignoreCase = true) ||
                    message.contains("timed out", ignoreCase = true) -> {
                "Kết nối quá thời gian chờ"
            }

            else -> message
        }
    }
}
