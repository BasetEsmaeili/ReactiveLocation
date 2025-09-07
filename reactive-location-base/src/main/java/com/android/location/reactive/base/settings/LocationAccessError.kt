package com.android.location.reactive.base.settings

sealed interface LocationAccessError {
    data object Unknown : LocationAccessError
    data object PermissionDenied : LocationAccessError
    data object LocationNotEnabled : LocationAccessError
    data object ServiceNotAvailable : LocationAccessError
    data object LocationEnableRequestCancelled : LocationAccessError
    data object NoProviderAvailable : LocationAccessError
}
