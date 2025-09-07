package com.android.location.reactive.base.permission

import android.content.Context

interface PermissionCheck {
    val permissions: Array<String>

    fun isPermissionAvailable(context: Context): Boolean
}
