package com.android.location.reactive.base

sealed interface LocationEnableRequestResult {
    data object Success : LocationEnableRequestResult
    data class Error(val e: Throwable) : LocationEnableRequestResult
}