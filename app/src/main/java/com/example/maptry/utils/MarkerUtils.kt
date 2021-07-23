package com.example.maptry.utils

import android.util.Log
import com.example.maptry.activity.MapsActivity
import com.example.maptry.activity.MapsActivity.Companion.addrThread
import com.example.maptry.activity.MapsActivity.Companion.geocoder
import com.example.maptry.activity.MapsActivity.Companion.listAddr
import com.example.maptry.activity.MapsActivity.Companion.mMap
import com.example.maptry.activity.MapsActivity.Companion.myList
import com.example.maptry.activity.MapsActivity.Companion.myLive
import com.example.maptry.activity.MapsActivity.Companion.mymarker
import com.example.maptry.changeUI.gson
import com.example.maptry.dataclass.*
import com.example.maptry.server.addLivePOI
import com.example.maptry.server.addPOI
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import org.json.JSONObject
import java.io.IOException

fun createLiveMarker(liveMarker: JSONObject){
    val address = liveMarker.get("address") as String
    listAddr = geocoder.getFromLocationName(address, 1)
    val lat = listAddr?.get(0)?.latitude!!
    val lon = listAddr?.get(0)?.longitude!!
    val point = LatLng(
        lat,
        lon
    )
    val mark = createMarker(point)
    mymarker.put(point.toString(), mark)
    myLive.put(point.toString(), liveMarker)
    myList.put(point.toString(), liveMarker)
    mark?.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
}

fun createUserMarker(userMarker: JSONObject){
    val address = userMarker.get("address") as String
    listAddr = geocoder.getFromLocationName(address, 1)
    val lat = listAddr?.get(0)?.latitude!!
    val lon = listAddr?.get(0)?.longitude!!
    val point = LatLng(
        lat,
        lon
    )
    val mark = createMarker(point)
    mymarker.put(point.toString(), mark)
    myList.put(point.toString(), userMarker)
    mark?.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
}


fun createJsonMarker(
    name: String,
    address: String,
    content: String,
    type: String,
    lat: String,
    lon: String,
    phone: String,
    url: String,
    id: String
): JSONObject {

    val userMark = UserMarker(name, address, content, type, lat, lon, url, phone, "temp")
    val addPOIClass = AddPointOfInterest(id, userMark)
    val jsonToAdd = gson.toJson(addPOIClass)
    val markId = addPOI(jsonToAdd)
    userMark.markId = markId

    return JSONObject(gson.toJson(userMark))
}
fun createLiveJsonMarker(
    name: String,
    address: String,
    timer: String,
    id: String
): JSONObject {

    val userLive = UserLive(name, address, timer,id)
    val addPOIClass = AddLiveEvent(id, userLive)
    val jsonToAdd = gson.toJson(addPOIClass)
    addLivePOI(jsonToAdd)
    val userLiveAdded = UserLiveAdded(name, address, timer,id)

    return JSONObject(gson.toJson(userLiveAdded))
}

fun createMarker(p0: LatLng): Marker? {

    val background = object : Runnable {
        override fun run() {
            try {
                listAddr = geocoder.getFromLocation(p0.latitude, p0.longitude, 1)
                return
            } catch (e: IOException) {
                Log.e("Error", "grpc failed2: " + e.message, e)
                // ... retry again your code that throws the exeception
            }
        }

    }
    addrThread = Thread(background)
    addrThread?.start()

    try {
        addrThread?.join()
    } catch (e:InterruptedException) {
        e.printStackTrace()
    }


    val text = "Indirizzo:" + listAddr?.get(0)?.getAddressLine(0)+"\nGeoLocalita:" +  listAddr?.get(0)?.locality + "\nAdminArea: " + listAddr?.get(0)?.adminArea + "\nCountryName: " + listAddr?.get(0)?.countryName + "\nPostalCode: " + listAddr?.get(0)?.postalCode + "\nFeatureName: " + listAddr?.get(0)?.featureName

    val x = mMap.addMarker(
        MarkerOptions()
            .position(p0)
            .title(text)
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            .alpha(0.7f)
    )

    mymarker.put(p0.toString(),x)
    return x
}