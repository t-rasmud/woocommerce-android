package com.woocommerce.android

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Injectable wrapper around AppPrefs.
 *
 * AppPrefs interface consists of static methods, which make the client code difficult to test/mock.
 * Main purpose of this wrapper is to make testing easier.
 *
 */
@Singleton
class AppPrefsWrapper @Inject constructor() {
    var selectedShipmentProviderName: String
        get() = AppPrefs.getSelectedShipmentTrackingProviderName()
        set(value) = AppPrefs.setSelectedShipmentTrackingProviderName(value)

    var isSelectedShipmentTrackingProviderCustom: Boolean
        get() = AppPrefs.getIsSelectedShipmentTrackingProviderCustom()
        set(value) = AppPrefs.setIsSelectedShipmentTrackingProviderNameCustom(value)
}
