package it.unibo.socialplaces.config

import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationSettingsRequest

object Location {
    fun createLocationRequest(): LocationRequest = LocationRequest.create().apply {
        interval = 5000
        fastestInterval = 2000
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    fun createLocationSettingsRequest(): LocationSettingsRequest = LocationSettingsRequest
        .Builder()
        .addLocationRequest(createLocationRequest())
        .build()
}