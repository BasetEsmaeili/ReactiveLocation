package com.android.location.reactive.base.result

import androidx.activity.result.IntentSenderRequest

sealed interface LocationResolveResult {
    data class EnableLocationByShowDialog(val senderRequest: IntentSenderRequest) :
        LocationResolveResult

    data object EnableLocationBySettingActivity : LocationResolveResult
    data object PermissionRequired : LocationResolveResult
    data object FailedToEnableLocation : LocationResolveResult
}