package com.android.location.reactive.base.result

import android.location.Location

sealed interface LocationEvent {
    data object ProviderDisabled : LocationEvent

    data object ProviderEnabled : LocationEvent

    data class LocationChanged(val location: Location?) : LocationEvent
}
