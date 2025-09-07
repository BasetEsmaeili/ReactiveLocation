package com.android.location.reactive.base

import android.location.Location
import com.android.location.reactive.base.configuration.FetchConfiguration
import com.android.location.reactive.base.data.LocationEvent
import com.android.location.reactive.base.exception.LocationNotEnabledException
import com.android.location.reactive.base.exception.PermissionDeniedException
import com.android.location.reactive.base.permission.PermissionCheck
import com.android.location.reactive.base.settings.LocationEnableRequestResult
import com.android.location.reactive.base.settings.LocationAccessError
import kotlinx.coroutines.flow.Flow

abstract class LocationService<T : FetchConfiguration>(
    private val context: android.content.Context,
    private val permissionCheck: PermissionCheck
) {

    /**
     * Checks if location permission is granted.
     *
     * @return `true` if location permission is granted, `false` otherwise.
     */
    val isLocationPermissionAvailable: Boolean
        get() = permissionCheck.isPermissionAvailable(context)

    /**
     * The configuration for fetching location.
     *
     * This property allows you to customize various aspects of how location is retrieved,
     * such as the desired accuracy, update interval, and other provider-specific settings.
     *
     * **Important:** You must assign a value to this property before starting location updates.
     * If no value is assigned, a default configuration will be used, which might not be optimal
     * for your specific use case.
     */
    abstract var fetchConfiguration: T

    /**
     * Exception that is used when service is not available (e.g. Google Play Services for FusedLocationProvider).
     * @see isServiceAvailable
     */
    protected abstract val serviceNotAvailableException: Throwable

    /**
     * Determines if a location service (e.g., Google Play Service, or Android System, or HMS) is available.
     */
    protected abstract val isServiceAvailable: Boolean

    /**
     * A [Flow] that emits the last known location.
     *
     * This flow provides access to the most recently recorded location, which might be stale
     * or null if no location has been determined yet. It's useful for quickly obtaining a
     * location without initiating a new location request, but its accuracy and timeliness
     * are not guaranteed.
     *
     * @throws PermissionDeniedException if the location permission is not available.
     * @throws serviceNotAvailableException if the underlying location service (e.g., Google Play Services
     * for FusedLocationProvider) is not available on the device. The specific type of this exception
     * depends on the service provider.
     * @see isLocationPermissionAvailable
     * @see isServiceAvailable
     * @see mapToLocationAccessError
     */
    abstract val lastKnownLocationFlow: Flow<Location?>

    /**
     * A [Flow] that emits the current location.
     *
     * This flow attempts to retrieve the current location based on the [fetchConfiguration] that you have provided.
     * It may involve actively requesting a new location fix,
     * which can take some time and consume more power than accessing the last known location.
     * The emitted value will be:
     * - The current [Location] if successfully retrieved.
     * - `null` if the location cannot be determined (e.g., no signal is available).
     *
     * This flow typically completes after emitting a single location or `null`. For continuous
     * location updates, use [locationUpdatesFlow].
     *
     * @throws PermissionDeniedException if the necessary location permissions have not been granted.
     * @throws serviceNotAvailableException if the underlying location service (e.g., Google Play Services
     * for FusedLocationProvider) is not available on the device. The specific type of this exception
     * depends on the service provider.
     * @throws LocationNotEnabledException if location services are disabled on the device.
     * @see isLocationPermissionAvailable
     * @see isServiceAvailable
     * @see isLocationAvailable
     * @see requestToEnableLocation
     * @see mapToLocationAccessError
     * @see fetchConfiguration
     */
    abstract val currentLocationFlow: Flow<Location?>


    /**
     * A [Flow] that emits location updates.
     *
     * This flow provides a continuous stream of [LocationEvent]s, delivering updates
     * as they become available based on the [fetchConfiguration]. This is suitable for
     * applications that need to track location changes over time or be notified of
     * location provider status changes.
     *
     * The emitted [LocationEvent] can be one of the following:
     * - [LocationEvent.LocationChanged]: Indicates a new [Location] has been received.
     *   The contained `location` might be `null` if the provider is unable to determine
     *   the current location.
     * - [LocationEvent.ProviderDisabled]: Indicates that the location provider currently
     *   being used has become disabled (e.g., the user turned off GPS).
     * - [LocationEvent.ProviderEnabled]: Indicates that the location provider currently
     *   being used has become enabled.
     *
     * Unlike [currentLocationFlow] or [lastKnownLocationFlow], this flow does not complete
     * after a single emission and will continue to emit updates until the collecting coroutine
     * is cancelled.
     *
     * @throws PermissionDeniedException if the necessary location permissions have not been granted.
     * @throws serviceNotAvailableException if the underlying location service (e.g., Google Play Services
     * for FusedLocationProvider) is not available on the device. The specific type of this exception
     * depends on the service provider.
     * @see isLocationPermissionAvailable
     * @see isServiceAvailable
     * @see isLocationAvailable
     * @see requestToEnableLocation
     * @see mapToLocationAccessError
     * @see fetchConfiguration
     * @see LocationEvent
     * @see LocationEvent.LocationChanged
     * @see LocationEvent.ProviderDisabled
     * @see LocationEvent.ProviderEnabled
     */
    abstract val locationUpdatesFlow: Flow<LocationEvent>

    /**
     * Checks if location services are currently available and enabled on the device.
     *
     * This function determines whether the device's location settings allow for location
     * retrieval. It does not check for location permissions; for that, use
     * [isLocationPermissionAvailable].
     *
     * @return `true` if location services are enabled, `false` otherwise.
     * @see isLocationPermissionAvailable
     * @see requestToEnableLocation
     * @see mapToLocationAccessError
     */
    abstract suspend fun isLocationAvailable(): Boolean

    /**
     * Requests the user to enable location services if they are currently disabled.
     *
     * This function will typically present a system dialog to the user, prompting them
     * to turn on location services. The result of this request is encapsulated in a
     * [LocationEnableRequestResult].
     *
     * - [LocationEnableRequestResult.Success]: Location services were successfully enabled by the user.
     * - [LocationEnableRequestResult.Failure]: The user declined to enable location services.
     *
     * @return A [LocationEnableRequestResult] indicating the outcome of the request.
     * @see isLocationAvailable
     * @see LocationEnableRequestResult
     */
    abstract suspend fun requestToEnableLocation(): LocationEnableRequestResult

    /**
     * Maps a given [Throwable] to a corresponding [LocationAccessError].
     *
     * This function helps in categorizing different types of exceptions that can occur
     * during location access into a more manageable set of error types defined by
     * [LocationAccessError].
     *
     * It handles common errors like [PermissionDeniedException] and [LocationNotEnabledException]
     * directly. For other exceptions, it delegates to [getServiceSpecificLocationAccessError]
     * to allow for provider-specific error mapping.
     *
     * @param exception The [Throwable] that occurred during location access.
     * @return The corresponding [LocationAccessError].
     * @see LocationAccessError
     */
    suspend fun mapToLocationAccessError(exception: Throwable): LocationAccessError {
        return when (exception) {
            is PermissionDeniedException -> LocationAccessError.PermissionDenied
            is LocationNotEnabledException -> LocationAccessError.LocationNotEnabled

            else -> getServiceSpecificLocationAccessError(exception)
        }
    }

    protected abstract suspend fun getServiceSpecificLocationAccessError(exception: Throwable): LocationAccessError
}
