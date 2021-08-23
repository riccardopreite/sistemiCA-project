package com.example.maptry.config

import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.model.LatLng
import java.io.Serializable

interface GetPointOfInterestDetail<T : AppCompatActivity> : Serializable {
    fun getPointOfInterestDetail(activity: T, position: LatLng)
}