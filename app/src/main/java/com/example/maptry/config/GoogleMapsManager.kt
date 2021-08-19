package com.example.maptry.config

import android.location.Location
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.maptry.activity.MapsActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import java.util.function.Function

class GoogleMapsManager(
    private var getPointOfInterestDetail: GetPointOfInterestDetail<AppCompatActivity>? = null,
    private var createPointOfInterestOrLiveEvent: CreatePointOfInterestOrLiveEvent<AppCompatActivity>? = null
)
    : OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener {
    val TAG = GoogleMapsManager::class.qualifiedName

    lateinit var googleMap: GoogleMap

    var lastLocation: Location? = null

    override fun onMapReady(map: GoogleMap) {
        Log.v(TAG, "onMapReady")
        googleMap = map
        map.setOnMapClickListener(this)
    }

    override fun onMapClick(position: LatLng) {
        Log.v(TAG, "onMapClick")

//        createPointOfInterestOrLiveEvent?.createPointOfInterestOrLiveEvent(position)
    }

    override fun onMarkerClick(p0: Marker): Boolean {
        Log.v(TAG, "onMarkerClick")

//        getPointOfInterestDetail(p0.position)

        return true
    }
}