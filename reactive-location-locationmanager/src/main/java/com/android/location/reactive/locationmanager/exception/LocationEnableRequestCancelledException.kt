package com.android.location.reactive.locationmanager.exception

class LocationEnableRequestCancelledException(override val message: String? = "Location enable request was cancelled by the user") :
    Exception(message)
