package com.android.location.reactive.base

class PermissionDeniedException(override val message: String? = "Required Permission denied") :
    Exception(message)