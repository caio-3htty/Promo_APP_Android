package com.prumo.core.error

sealed class AppError(message: String, cause: Throwable? = null) : Throwable(message, cause) {
    data class AuthError(val reason: String) : AppError(reason)
    data class PermissionError(val reason: String) : AppError(reason)
    data class ValidationError(val reason: String) : AppError(reason)
    data class NetworkError(val reason: String, val code: Int? = null) : AppError(reason)
}