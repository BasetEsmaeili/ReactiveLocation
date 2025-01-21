package com.android.location.reactive.base.exception

class PermissionDeniedException(override val message: String? = "Required Permission denied") :
    Exception(message)