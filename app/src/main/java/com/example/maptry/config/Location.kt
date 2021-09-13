package com.example.maptry.config

import android.location.Geocoder
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.maps.GoogleMap

object Location {
    const val REQUEST_LOCATION_PERMISSIONS = 1
    const val REQUEST_CHECK_SETTINGS = 2

    fun createLocationRequest(): LocationRequest {
        return LocationRequest.create().apply {
            interval = 20000
            fastestInterval = 10000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    fun createLocationSettingsRequest(): LocationSettingsRequest {
        // TODO Check
        return LocationSettingsRequest
            .Builder()
            .addLocationRequest(createLocationRequest())
            .build()
    }
}