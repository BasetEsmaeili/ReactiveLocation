package com.android.location.reactive.locationmanager.exception

class NoProviderAvailableException(override val message: String? = "No provider available to use!") :
    Exception(message)
