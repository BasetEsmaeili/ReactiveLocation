package com.android.location.reactive.locationmanager

import android.location.Location

interface LocationServiceBridge {
    fun onLocationChanged(location: Location)
    fun onProviderEnabled(provider: String)
    fun onProviderDisabled(provider: String)
}
