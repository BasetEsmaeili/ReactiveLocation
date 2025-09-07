package com.android.location.reactive.base.permission

import android.Manifest
import android.content.Context
import com.android.location.reactive.core.isPermissionGranted

class FineAndCoarseLocationPermissionCheck : PermissionCheck {
    override val permissions: Array<String>
        get() = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

    override fun isPermissionAvailable(context: Context): Boolean {
        return context.isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION) &&
                context.isPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION)
    }
}
