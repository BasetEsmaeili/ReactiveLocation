package com.android.location.reactive.base.settings

sealed interface LocationEnableRequestResult {
    data object Success : LocationEnableRequestResult
    data class Failure(val e: Throwable) : LocationEnableRequestResult
}