package com.android.location.reactive.locationmanager

enum class LocationQuality {
    /**
     * A quality constant indicating a location provider may choose to satisfy this request by
     * providing less accurate locations in order to save power. Each location provider may
     * interpret this field differently, but as an example, the network provider may choose to
     * return cell based locations rather than wifi based locations in order to save power when this
     * flag is present.
     */
    LOW_POWER,

    /**
     * A quality constant indicating a location provider may choose to satisfy this request by
     * equally balancing power and accuracy constraints. Each location provider may interpret this
     * field differently, but location providers will generally use their default behavior when this
     * flag is present.
     */
    BALANCED_POWER_ACCURACY,

    /**
     * A quality constant indicating a location provider may choose to satisfy this request by
     * providing very accurate locations at the expense of potentially increased power usage. Each
     * location provider may interpret this field differently, but as an example, the network
     * provider may choose to return only wifi based locations rather than cell based locations in
     * order to have greater accuracy when this flag is present.
     */
    HIGH_ACCURACY
}
