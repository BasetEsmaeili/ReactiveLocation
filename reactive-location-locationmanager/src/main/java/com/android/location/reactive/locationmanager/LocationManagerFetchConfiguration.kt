package com.android.location.reactive.locationmanager

import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import com.android.location.reactive.base.configuration.FetchConfiguration
import kotlin.math.min

/**
 * Configuration for fetching location using [AndroidLocationManagerService].
 *
 * @param provider The type of the provider to use. The default is [LocationProvider.PASSIVE].
 * @param intervalMillis The request interval may be set to [Long.MAX_VALUE] which indicates this
 * request will not actively generate location updates (and thus will not be power blamed for location),
 * but may receive location updates generated as a result of other location requests.
 * A passive request must always have an explicit minimum update interval set. The default is [Long.MAX_VALUE].
 * Locations may be available at a faster interval than specified here, see [mMinUpdateIntervalMillis] for the behavior in that case.
 * @param quality The quality of the location request. The quality is a hint to providers on how they should weigh power vs accuracy tradeoffs.
 * High accuracy locations may cost more power to produce, and lower accuracy locations may cost less power to produce.
 * The default is [LocationQuality.BALANCED_POWER_ACCURACY].
 * @param durationMillis Sets the duration this request will continue before being automatically removed. Defaults
 * to [Long.MAX_VALUE], which represents an unlimited duration.
 * @param maxUpdates Sets the maximum number of location updates for this request before this request is
 * automatically removed. Defaults to [Integer.MAX_VALUE], which represents an
 * unlimited number of updates.
 * @param mMinUpdateIntervalMillis Sets an explicit minimum update interval. If location updates are available faster than
 * the request interval then an update will only occur if the minimum update interval has
 * expired since the last location update. Defaults to no explicit minimum update interval
 * set, which means some sensible default between 0 and the interval will be chosen. The
 * exact value is not specified at the moment. If an exact known value is required, clients
 * should set an explicit value themselves.
 * Note: Some allowance for jitter is already built into the minimum update interval, so you need not worry about updates blocked simply because they
 * arrived a fraction of a second earlier than expected.
 * Note: The minimum of the interval and the minimum update interval will be used as the minimum update interval.
 * @param minUpdateDistanceMeters Sets the minimum update distance between location updates. If a potential location
 * update is closer to the last location update than the minimum update distance, then
 * the potential location update will not occur. Defaults to 0, which represents no minimum
 * update distance.
 * @param maxUpdateDelayMillis Sets the maximum time any location update may be delayed, and thus grouped with following
 * updates to enable location batching. If the maximum update delay is equal to or greater
 * than twice the interval, then location providers may provide batched results. Defaults to 0, which represents no batching allowed.
 */
data class LocationManagerFetchConfiguration(
    val provider: LocationProvider = LocationProvider.PASSIVE,
    val intervalMillis: Long = PASSIVE_INTERVAL,
    val quality: LocationQuality = LocationQuality.BALANCED_POWER_ACCURACY,
    val durationMillis: Long = Long.MAX_VALUE,
    @field:IntRange(0, Int.MAX_VALUE.toLong())
    val maxUpdates: Int = Int.MAX_VALUE,
    private val mMinUpdateIntervalMillis: Long = IMPLICIT_MIN_UPDATE_INTERVAL,
    @field:FloatRange(0.0, Float.MAX_VALUE.toDouble())
    val minUpdateDistanceMeters: Float = 0f,
    @field:IntRange(0, Long.MAX_VALUE)
    val maxUpdateDelayMillis: Long = 0
) : FetchConfiguration {

    companion object {
        private const val PASSIVE_INTERVAL = Long.MAX_VALUE
        private const val IMPLICIT_MIN_UPDATE_INTERVAL = -1L
        const val IMPLICIT_MIN_UPDATE_INTERVAL_FACTOR: Double = 1.0 / 6.0
    }

    init {
        check(
            intervalMillis != PASSIVE_INTERVAL
                    || mMinUpdateIntervalMillis != IMPLICIT_MIN_UPDATE_INTERVAL
        ) {
            "passive location requests must have an explicit minimum update interval"
        }
    }


    val minUpdateIntervalMillis: Long
        get() {
            return if (mMinUpdateIntervalMillis == IMPLICIT_MIN_UPDATE_INTERVAL) {
                (intervalMillis * IMPLICIT_MIN_UPDATE_INTERVAL_FACTOR).toLong()
            } else {
                // the min is only necessary in case someone use a deprecated function to mess with the
                // interval or min update interval
                min(mMinUpdateIntervalMillis.toDouble(), intervalMillis.toDouble()).toLong()
            }
        }
}
