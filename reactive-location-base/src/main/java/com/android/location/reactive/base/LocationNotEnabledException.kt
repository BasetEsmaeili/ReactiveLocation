package com.android.location.reactive.base

class LocationNotEnabledException(override val message: String? = "Location is not enabled") :
    Exception(message)