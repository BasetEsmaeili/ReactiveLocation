package com.android.location.reactive.base

import android.Manifest
import android.content.Context
import android.location.Location
import com.android.location.reactive.base.configuration.FetchConfiguration
import com.android.location.reactive.base.configuration.PermissionType
import com.android.location.reactive.base.exception.LocationNotEnabledException
import com.android.location.reactive.base.exception.PermissionDeniedException
import com.android.location.reactive.base.result.LocationEnableRequestResult
import com.android.location.reactive.base.result.LocationEvent
import com.android.location.reactive.base.result.LocationResolveResult
import com.android.location.reactive.core.isPermissionGranted
import kotlinx.coroutines.flow.Flow

abstract class LocationService<T : FetchConfiguration>(private val context: Context) {

    var permissionType: PermissionType = PermissionType.ACCESS_COARSE_LOCATION

    abstract var fetchConfiguration: T

    protected abstract val serviceNotAvailableException: Throwable

    protected abstract val isServiceAvailable: Boolean

    abstract val lastKnownLocationFlow: Flow<Location?>

    abstract val currentLocationFlow: Flow<Location?>

    abstract val locationUpdatesFlow: Flow<LocationEvent>

    abstract suspend fun isLocationAvailable(): Boolean

    abstract suspend fun requestToEnableLocation(): LocationEnableRequestResult

    suspend fun resolveLocationError(exception: Throwable): LocationResolveResult {
        return when (exception) {
            is LocationNotEnabledException -> LocationResolveResult.EnableLocationBySettingActivity
            is PermissionDeniedException -> LocationResolveResult.PermissionRequired
            else -> resolveSpecificServiceLocationError(exception)
        }
    }

    protected abstract suspend fun resolveSpecificServiceLocationError(exception: Throwable): LocationResolveResult

    fun isLocationPermissionAvailable(): Boolean {
        val isFineLocationAvailable =
            context.isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)
        val isCoarseLocationAvailable =
            context.isPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION)
        return when (permissionType) {
            PermissionType.ACCESS_FINE_LOCATION -> isFineLocationAvailable
            PermissionType.ACCESS_COARSE_LOCATION -> isCoarseLocationAvailable
            PermissionType.BOTH -> isFineLocationAvailable && isCoarseLocationAvailable
        }
    }

    suspend fun isLocationReadyToFetch(): Boolean {
        return isLocationPermissionAvailable() && isLocationAvailable()
    }
}
