package com.android.location.reactive.locationmanager.exception

class AndroidLocationManagerNotAvailableException(override val message: String? = "Android Location Manager is not available") :
    Exception(message)