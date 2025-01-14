package com.android.location.reactive.core

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

val isSdk23AndUp: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

val isSdk26AndUp: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

val isSdk28AndUp: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

val isSdk29AndUp: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

val isSdk30AndUp: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

val isSdk31AndUp: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

val isSdk33AndUp: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

val isSdk34AndUp: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

inline fun <T> sdk33AndUp(action: () -> T): T? {
    return if (isSdk33AndUp) {
        action()
    } else {
        return null
    }
}

inline fun <T> sdk29AndUp(action: () -> T): T? {
    return if (isSdk29AndUp) {
        action()
    } else {
        return null
    }
}

inline fun <T> sdk30AndUp(action: () -> T): T? {
    return if (isSdk30AndUp) {
        action()
    } else {
        null
    }
}

fun Context.isPermissionGranted(permission: String) =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED