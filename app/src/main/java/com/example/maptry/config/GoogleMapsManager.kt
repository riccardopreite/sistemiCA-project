package com.example.maptry.config

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback

object GoogleMapsOnMapReadyCallback : OnMapReadyCallback {
    override fun onMapReady(map: GoogleMap) {
        map.setOnMapClickListener {

        }
    }
}