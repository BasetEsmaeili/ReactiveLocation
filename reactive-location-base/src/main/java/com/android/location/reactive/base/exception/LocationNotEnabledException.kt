package com.android.location.reactive.base.exception

class LocationNotEnabledException(override val message: String? = "Location is not enabled") :
    Exception(message)