package com.example.maptry.config

import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.model.LatLng
import java.io.Serializable

interface CreatePointOfInterestOrLiveEvent<T: AppCompatActivity> : Serializable {
    fun createPointOfInterestOrLiveEvent(activity: AppCompatActivity, position: LatLng)
}