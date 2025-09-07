package com.android.location.reactive.locationmanager

import android.annotation.SuppressLint
import android.app.Activity.RESULT_CANCELED
import android.content.Intent
import android.location.Criteria
import android.location.Location
import android.location.provider.ProviderProperties
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import com.android.location.reactive.base.LocationService
import com.android.location.reactive.base.data.LocationEvent
import com.android.location.reactive.base.exception.LocationNotEnabledException
import com.android.location.reactive.base.exception.PermissionDeniedException
import com.android.location.reactive.base.permission.PermissionCheck
import com.android.location.reactive.base.settings.LocationEnableRequestResult
import com.android.location.reactive.base.settings.LocationAccessError
import com.android.location.reactive.core.lazyFast
import com.android.location.reactive.locationmanager.exception.AndroidLocationManagerNotAvailableException
import com.android.location.reactive.locationmanager.exception.LocationEnableRequestCancelledException
import com.android.location.reactive.locationmanager.exception.NoProviderAvailableException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AndroidLocationManagerService(
    private val activity: ComponentActivity,
    permissionCheck: PermissionCheck
) : LocationService<LocationManagerFetchConfiguration>(
    activity.applicationContext,
    permissionCheck
) {

    override val serviceNotAvailableException: Throwable
        get() = AndroidLocationManagerNotAvailableException()

    override val isServiceAvailable: Boolean
        get() = locationManager != null

    private val safeLocationManager
        get() = locationManager ?: throw serviceNotAvailableException

    private val locationManager by lazyFast {
        ContextCompat.getSystemService(
            activity.applicationContext,
            android.location.LocationManager::class.java
        )
    }

    override var fetchConfiguration: LocationManagerFetchConfiguration =
        LocationManagerFetchConfiguration()

    override val lastKnownLocationFlow: Flow<Location?>
        @SuppressLint("MissingPermission")
        get() = flow {
            if (!isLocationPermissionAvailable) {
                throw PermissionDeniedException()
            }
            emit(safeLocationManager.getLastKnownLocation(fetchConfiguration.provider.mapToLocationRequestProvider()))
        }

    override val currentLocationFlow: Flow<Location?>
        @SuppressLint("MissingPermission")
        get() = callbackFlow {
            if (!isLocationPermissionAvailable) {
                close(PermissionDeniedException())
            }
            if (!isServiceAvailable) {
                close(serviceNotAvailableException)
            }

            if (!isLocationAvailable()) {
                close(LocationNotEnabledException())
            }

            val cancellationSignal = android.os.CancellationSignal()
            try {
                LocationManagerCompat.getCurrentLocation(
                    safeLocationManager,
                    fetchConfiguration.provider.mapToLocationRequestProvider(),
                    cancellationSignal,
                    ContextCompat.getMainExecutor(activity.applicationContext)
                ) {
                    trySend(it)
                    close()
                }
            } catch (e: Exception) {
                close(e)
            }
            awaitClose {
                cancellationSignal.cancel()
            }
        }

    override val locationUpdatesFlow: Flow<LocationEvent>
        @SuppressLint("MissingPermission")
        get() = callbackFlow {
            if (!isLocationPermissionAvailable) {
                close(PermissionDeniedException())
            }
            if (!isServiceAvailable) {
                close(serviceNotAvailableException)
            }

            if (isLocationAvailable()) {
                trySend(LocationEvent.ProviderEnabled)
            } else {
                trySend(LocationEvent.ProviderDisabled)
            }

            val locationListener = LocationListenerImpl(object : LocationServiceBridge {
                override fun onLocationChanged(location: Location) {
                    trySend(LocationEvent.LocationChanged(location))
                }

                override fun onProviderEnabled(provider: String) {
                    trySend(LocationEvent.ProviderEnabled)
                }

                override fun onProviderDisabled(provider: String) {
                    trySend(LocationEvent.ProviderDisabled)
                }
            })
            try {
                LocationManagerCompat.requestLocationUpdates(
                    safeLocationManager,
                    fetchConfiguration.provider.mapToLocationRequestProvider(),
                    prepareLocationRequest(),
                    ContextCompat.getMainExecutor(activity.applicationContext),
                    locationListener
                )
            } catch (e: Exception) {
                close(e)
            }
            awaitClose { safeLocationManager.removeUpdates(locationListener) }
        }

    override suspend fun isLocationAvailable(): Boolean {
        return with(fetchConfiguration) {
            hasProvider(provider) && isProviderEnabled(provider)
        }
    }

    override suspend fun requestToEnableLocation(): LocationEnableRequestResult {
        if (!isServiceAvailable) {
            return LocationEnableRequestResult.Failure(serviceNotAvailableException)
        }
        if (isLocationAvailable()) {
            return LocationEnableRequestResult.Success
        }
        runCatching { startSettingActivity() }.fold(
            onSuccess = { resultCode ->
                return if (isLocationAvailable()) {
                    LocationEnableRequestResult.Success
                } else {
                    LocationEnableRequestResult.Failure(
                        if (resultCode == RESULT_CANCELED) {
                            LocationEnableRequestCancelledException()
                        } else {
                            LocationNotEnabledException()
                        }
                    )
                }
            },
            onFailure = {
                return LocationEnableRequestResult.Failure(it)
            })
    }

    override suspend fun getServiceSpecificLocationAccessError(exception: Throwable): LocationAccessError {
        return when (exception) {
            is AndroidLocationManagerNotAvailableException -> LocationAccessError.ServiceNotAvailable
            is LocationEnableRequestCancelledException -> LocationAccessError.LocationEnableRequestCancelled
            is NoProviderAvailableException -> LocationAccessError.NoProviderAvailable
            else -> LocationAccessError.Unknown
        }
    }

    private suspend fun startSettingActivity(): Int = suspendCoroutine { continuation ->
        activity.activityResultRegistry.register(
            "AndroidLocationManagerService",
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            continuation.resume(result.resultCode)
        }.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    }

    private fun prepareLocationRequest() =
        LocationRequestCompat.Builder(fetchConfiguration.intervalMillis)
            .setDurationMillis(fetchConfiguration.durationMillis)
            .setMaxUpdateDelayMillis(fetchConfiguration.maxUpdateDelayMillis)
            .setMinUpdateDistanceMeters(fetchConfiguration.minUpdateDistanceMeters)
            .setMinUpdateIntervalMillis(fetchConfiguration.minUpdateIntervalMillis)
            .setQuality(
                when (fetchConfiguration.quality) {
                    LocationQuality.HIGH_ACCURACY -> LocationRequestCompat.QUALITY_HIGH_ACCURACY
                    LocationQuality.BALANCED_POWER_ACCURACY -> LocationRequestCompat.QUALITY_BALANCED_POWER_ACCURACY
                    LocationQuality.LOW_POWER -> LocationRequestCompat.QUALITY_LOW_POWER
                }
            )
            .build()

    private fun getCriteria() = Criteria().apply {
        horizontalAccuracy = when (fetchConfiguration.quality) {
            LocationQuality.HIGH_ACCURACY -> Criteria.ACCURACY_HIGH

            LocationQuality.BALANCED_POWER_ACCURACY -> Criteria.ACCURACY_MEDIUM

            LocationQuality.LOW_POWER -> Criteria.ACCURACY_LOW
        }
        isCostAllowed = fetchConfiguration.quality == LocationQuality.HIGH_ACCURACY
        powerRequirement = when (fetchConfiguration.quality) {
            LocationQuality.HIGH_ACCURACY -> Criteria.POWER_HIGH

            LocationQuality.BALANCED_POWER_ACCURACY -> Criteria.POWER_MEDIUM

            LocationQuality.LOW_POWER -> Criteria.POWER_LOW
        }
    }

    /**
     * Returns the best [LocationProvider] based on the current [fetchConfiguration].
     *
     * @param enabledOnly If `true`, only enabled providers will be considered. Defaults to `true`.
     * @return The best [LocationProvider] based on the configuration.
     * @throws NoProviderAvailableException if no suitable provider is found.
     * @throws AndroidLocationManagerNotAvailableException if the location manager service is not available.
     */
    fun getBestProviderBasedOnConfiguration(enabledOnly: Boolean = true): LocationProvider =
        (safeLocationManager).getBestProvider(
            getCriteria(),
            enabledOnly
        )?.let { LocationProvider.mapFrom(it) } ?: throw NoProviderAvailableException()

    /**
     * Determines if a specific location provider is available on the device.
     *
     * @param locationProvider The [LocationProvider] to check.
     * @return `true` if the provider is available, `false` otherwise.
     * @throws AndroidLocationManagerNotAvailableException if the location manager service is not available.
     */
    fun hasProvider(locationProvider: LocationProvider): Boolean {
        return LocationManagerCompat.hasProvider(
            safeLocationManager,
            locationProvider.mapToLocationRequestProvider()
        )
    }

    /**
     * Determines if a specific location provider is enabled.
     *
     * @param provider The [LocationProvider] to check.
     * @return `true` if the provider is enabled, `false` otherwise.
     * @throws AndroidLocationManagerNotAvailableException if the location manager service is not available.
     */
    fun isProviderEnabled(provider: LocationProvider): Boolean =
        safeLocationManager.isProviderEnabled(provider.mapToLocationRequestProvider())

    /**
     * Retrieves the properties of a location provider.
     *
     * @param provider The [LocationProvider] whose properties are to be retrieved.
     * @return The [ProviderProperties] for the given provider.
     * @throws NoProviderAvailableException if the properties for the provider are not available
     * or the provider does not exist.
     * @throws AndroidLocationManagerNotAvailableException if the location manager service is not available.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    fun getProviderProperties(provider: LocationProvider): ProviderProperties {
        return safeLocationManager.getProviderProperties(
            provider.mapToLocationRequestProvider()
        ) ?: throw NoProviderAvailableException()
    }

    /**
     *  Returns the current enabled/disabled state of location in device setting.
     */
    val isLocationSettingEnabled: Boolean
        get() = LocationManagerCompat.isLocationEnabled(safeLocationManager)

    /**
     * Returns a list of the names of available location providers. If [enabledOnly] is false,
     *
     * @param enabledOnly if `true` then only enabled providers are included.
     * @return list of providers.
     */
    fun getProviders(enabledOnly: Boolean): List<LocationProvider> {
        return safeLocationManager
            .getProviders(enabledOnly)
            .mapNotNull {
                LocationProvider.mapFrom(it)
            }
    }

    /**
     * Sends an extra command to a location provider.
     * Can be used to support provider specific extensions to the Location Manager API
     *
     * @param provider The [LocationProvider] to send the command to.
     * @param command Name of the command to send to the provider.
     * @param extras Optional arguments for the command, or null.
     * @return `true` always, the return value may be ignored.
     */
    fun sendExtraCommand(
        provider: LocationProvider,
        command: String,
        extras: Bundle? = null
    ): Boolean {
        return safeLocationManager.sendExtraCommand(
            provider.mapToLocationRequestProvider(),
            command,
            extras
        )
    }

    /**
     * Returns the current list of GNSS antenna infos, or null if unknown or unsupported.
     * @see gnssCapabilities
     */
    val gnssAntennaInfos: List<android.location.GnssAntennaInfo>?
        @RequiresApi(Build.VERSION_CODES.S)
        get() = safeLocationManager.gnssAntennaInfos

    /**
     * Returns the supported capabilities of the GNSS chipset.
     */
    val gnssCapabilities: android.location.GnssCapabilities
        @RequiresApi(Build.VERSION_CODES.S)
        get() = safeLocationManager.gnssCapabilities

    /**
     * Returns the model name of the GNSS hardware.
     */
    val gnssHardwareModelName
        get() = LocationManagerCompat.getGnssHardwareModelName(safeLocationManager)

    /**
     * Returns the model year of the GNSS hardware.
     */
    val gnssYearOfHardware
        get() = LocationManagerCompat.getGnssYearOfHardware(safeLocationManager)
}