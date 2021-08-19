package com.example.maptry.config

import android.location.Geocoder
import com.google.android.gms.maps.GoogleMap

object Location {
    const val REQUEST_LOCATION_PERMISSIONS = 1
    const val REQUEST_CHECK_SETTINGS = 2

    lateinit var googleMap: GoogleMap

    lateinit var geocoder: Geocoder
}