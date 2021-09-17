package com.example.maptry.location

import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import android.view.View
import android.widget.LinearLayout
import android.widget.RelativeLayout
import com.example.maptry.R
import com.example.maptry.activity.MapsActivity.Companion.builder
import com.example.maptry.activity.MapsActivity.Companion.mapsActivityContext
import com.example.maptry.activity.MapsActivity.Companion.lastLocation
import com.example.maptry.activity.MapsActivity.Companion.locationCallback
import com.example.maptry.activity.MapsActivity.Companion.mLocationRequest
import com.example.maptry.activity.MapsActivity.Companion.mMap
import com.example.maptry.activity.MapsActivity.Companion.mymarker
import com.example.maptry.activity.MapsActivity.Companion.oldPos
import com.example.maptry.activity.MapsActivity.Companion.supportManager
import com.example.maptry.activity.MapsActivity.Companion.zoom
import com.example.maptry.utils.createMarker
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.Places

private const val UPDATE_INTERVAL = (10 * 1000).toLong()  /* 10 secs */
private const val FASTEST_INTERVAL: Long = 2000 /* 2 sec */

fun startLocationUpdates() {
    // initialize location request object
    mLocationRequest = LocationRequest.create()
    mLocationRequest!!.run {
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        interval = UPDATE_INTERVAL
        setFastestInterval(FASTEST_INTERVAL)
    }

    // initialize location setting request builder object
    builder.addLocationRequest(mLocationRequest!!)
    builder.setAlwaysShow(true)
    val locationSettingsRequest = builder.build()

    // initialize location service object
    val settingsClient = LocationServices.getSettingsClient(mapsActivityContext)
    settingsClient.checkLocationSettings(locationSettingsRequest)

    // call register location listener
    registerLocationListener()
}
fun registerLocationListener() {
    // initialize location callback object
    locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            onLocationChanged(locationResult!!.lastLocation)
        }
    }
}
// move position marker
fun onLocationChanged(location: Location) {

    val x = LatLng(location.latitude, location.longitude)
    try{
        oldPos?.remove()
    }
    catch (e:Exception){
        println("first time")
    }
    oldPos = createMarker(x)
    mymarker.remove(oldPos?.position.toString())

    if(zoom == 1){
        lastLocation = location
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(x, 17F))
        zoom = 0
    }
    lastLocation = location
}

fun isLocationEnable(): Boolean{
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        // This is new method provided in API 28
        val lm = mapsActivityContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        lm.isLocationEnabled;
    } else {
        // This is Deprecated in API 28
        val mode = Settings.Secure.getInt(mapsActivityContext.contentResolver, Settings.Secure.LOCATION_MODE,
            Settings.Secure.LOCATION_MODE_OFF);
        (mode != Settings.Secure.LOCATION_MODE_OFF);
    }
}

fun myLocationClick(contextUtils: Activity?): Boolean{
    if (isLocationEnable()){
        try{
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(
                lastLocation.latitude,
                lastLocation.longitude), 20F))
        }
        catch (e:Exception){}
    }
    else{
        val result: Task<LocationSettingsResponse> =
            contextUtils?.let {
                LocationServices.getSettingsClient(it)
                    .checkLocationSettings(builder.build())
            } as Task<LocationSettingsResponse>
        
        result.addOnCompleteListener { task ->
            try {
                val response: LocationSettingsResponse? =task.getResult(ApiException::class.java)
                println(response)
            } catch (exception: ApiException) {
                when (exception.statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED ->                             // Location settings are not satisfied. But could be fixed by showing the
                        try {
                            // Cast to a resolvable exception.
                            val resolvable: ResolvableApiException =
                                exception as ResolvableApiException
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            resolvable.startResolutionForResult(
                                contextUtils,
                                LocationRequest.PRIORITY_HIGH_ACCURACY
                            )
                        } catch (e: IntentSender.SendIntentException) {
                            // Ignore the error.
                        } catch (e: ClassCastException) {
                            // Ignore, should be an impossible error.
                        }
                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                    }
                }
            }
        }
    }
    return true
}

fun setUpMap(mapsKey: String) {
    Places.initialize(mapsActivityContext, mapsKey)
    val mapFragment =  supportManager.findFragmentById(R.id.map) as SupportMapFragment
    var locationButton : View? = mapFragment.view?.findViewById<LinearLayout>(Integer.parseInt("1"))
    val prov : View = (locationButton?.parent) as View
    locationButton = prov.findViewById(Integer.parseInt("2"))
    val layoutParams = locationButton?.layoutParams as RelativeLayout.LayoutParams
    layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0)
    layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
    layoutParams.setMargins(0, 0, 30, 30)
}