package com.android.location.reactive.locationmanager

import android.content.Context
import android.location.Location
import androidx.core.content.ContextCompat
import com.android.location.reactive.base.LocationEnableRequestResult
import com.android.location.reactive.base.LocationEvent
import com.android.location.reactive.base.LocationResolveResult
import com.android.location.reactive.base.LocationService
import com.android.location.reactive.core.lazyFast
import kotlinx.coroutines.flow.Flow

class AndroidLocationManagerService(context: Context) :
    LocationService<LocationManagerFetchConfiguration>(context) {

    private val locationManager by lazyFast {
        ContextCompat.getSystemService(context, android.location.LocationManager::class.java)
    }

    override var fetchConfiguration: LocationManagerFetchConfiguration =
        LocationManagerFetchConfiguration()

    override val serviceNotAvailableException: Throwable
        get() = AndroidLocationManagerNotAvailableException()
    override val isServiceAvailable: Boolean
        get() = locationManager != null
    override val lastKnownLocationFlow: Flow<Location?>
        get() = TODO("Not yet implemented")
    override val currentLocationFlow: Flow<Location?>
        get() = TODO("Not yet implemented")
    override val locationUpdatesFlow: Flow<LocationEvent>
        get() = TODO("Not yet implemented")

    override suspend fun isLocationAvailable(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun requestToEnableLocation(): LocationEnableRequestResult {
        TODO("Not yet implemented")
    }

    override suspend fun resolveSpecificServiceLocationError(exception: Throwable): LocationResolveResult {
        TODO("Not yet implemented")
    }
}