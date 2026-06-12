package io.github.juliasivridi.kindtube.util

sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    object Empty : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val type: ErrorType) : UiState<Nothing>()
}

enum class ErrorType {
    NO_NETWORK,
    QUOTA_EXCEEDED,
    AUTH_EXPIRED,
    GENERIC,
}
