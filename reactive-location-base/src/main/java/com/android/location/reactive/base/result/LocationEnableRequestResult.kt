package com.android.location.reactive.base.result

sealed interface LocationEnableRequestResult {
    data object Success : LocationEnableRequestResult
    data class Error(val e: Throwable) : LocationEnableRequestResult
}