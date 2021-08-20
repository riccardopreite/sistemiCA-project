package com.example.maptry.utils

import android.graphics.Color
import android.util.Log
import android.view.View
import android.widget.Toast
import com.example.maptry.R
import com.example.maptry.activity.MapsActivity
import com.example.maptry.activity.MapsActivity.Companion.addrThread
import com.example.maptry.activity.MapsActivity.Companion.alertDialog
import com.example.maptry.activity.MapsActivity.Companion.mapsActivityContext
import com.example.maptry.activity.MapsActivity.Companion.geocoder
import com.example.maptry.activity.MapsActivity.Companion.listAddr
import com.example.maptry.activity.MapsActivity.Companion.liveEventsList
import com.example.maptry.activity.MapsActivity.Companion.mMap
import com.example.maptry.activity.MapsActivity.Companion.myList
import com.example.maptry.activity.MapsActivity.Companion.myLive
import com.example.maptry.activity.MapsActivity.Companion.mymarker
import com.example.maptry.activity.MapsActivity.Companion.poisList
import com.example.maptry.api.Retrofit
import com.example.maptry.changeUI.gson
import com.example.maptry.config.Auth
import com.example.maptry.dataclass.*
import com.example.maptry.model.friends.AddFriendshipRequest
import com.example.maptry.model.liveevents.LiveEvent
import com.example.maptry.model.pointofinterests.PointOfInterest
import com.example.maptry.model.pointofinterests.RemovePointOfInterest
import com.example.maptry.server.addLivePOI
import com.example.maptry.server.addPOI
import com.example.maptry.server.removePOI
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

fun createLiveMarker(liveEvent: LiveEvent){
    val point = LatLng(
        liveEvent.latitude,
        liveEvent.longitude
    )
    CoroutineScope(Dispatchers.Main).launch {
        val mark = createMarker(point)
        mymarker.put(point.toString(), mark)
        liveEventsList.add(liveEvent)
        mark?.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
    }
}

fun createUserMarker(poi: PointOfInterest) {
    val point = LatLng(
        poi.latitude,
        poi.longitude
    )
    CoroutineScope(Dispatchers.Main).launch {
        val mark = createMarker(point)
        mymarker.put(point.toString(), mark) // Non eseguita ma va tenuta in una futura rimozione di createUserMarker(userMarker: JSONObject)
        poisList.add(poi)
        mark?.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
    }
}


fun createJsonMarker(
    name: String,
    address: String,
    type: String,
    visibility: String,
    lat: Double,
    lon: Double,
    phone: String,
    url: String,
    id: String
): JSONObject {

    val userMark = UserMarker(name, address, type, visibility, lat, lon, url, phone, "temp")
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

fun deletePOI(toRemove: String, view: View, showPOI:() -> Unit) {
    // poisToRemove is a list in this code but it can actually only be of size 0 or 1 because of how the api are implemented.
    val poisToRemove = poisList.filter { poi -> poi.name == toRemove }
    if(poisToRemove.isEmpty()) {
        return
    }
    var willDeletePoi = true
    val poiToRemove = poisToRemove.first()
    var markerId = LatLng(poiToRemove.latitude, poiToRemove.longitude).toString()
    val marker = (mymarker.get(markerId) as Marker)
    marker.remove()
    mymarker.remove(markerId)
    poisList.remove(poiToRemove)
    val userId = Auth.signInAccount?.email?.replace("@gmail.com","")!!

    val snackbar = Snackbar.make(view, R.string.removed_poi, 5000)
        .setAction(R.string.cancel) {
            willDeletePoi = false
            poisList.add(poiToRemove)
            mymarker.put(markerId, marker)
            Toast.makeText(
                mapsActivityContext,
                view.resources.getString(R.string.canceled_removal),
                Toast.LENGTH_LONG
            ).show()

            showPOI()
        }
    snackbar.setActionTextColor(Color.DKGRAY)
    val snackView = snackbar.view
    snackView.setBackgroundColor(Color.BLACK)
    snackbar.show()


    snackbar.addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
        override fun onShown(transientBottomBar: Snackbar?) {
            super.onShown(transientBottomBar)
        }
        override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
            super.onDismissed(transientBottomBar, event)
            if(!willDeletePoi) {
                return
            }
            //remove from db the poi
//            removePOI(poiToRemove.markId, userId)
            CoroutineScope(Dispatchers.IO).launch {
                val response = try {
                    Retrofit.pointOfInterestsApi.removePointOfInterest(RemovePointOfInterest(userId, poiToRemove.markId))
                } catch (e: IOException) {
                    e?.message?.let { it1 -> Log.e(MapsActivity.TAG, it1) }
                    return@launch
                } catch (e: HttpException) {
                    e?.message?.let { it1 -> Log.e(MapsActivity.TAG, it1) }
                    return@launch
                }

                if(response.isSuccessful) {
                    Log.i(MapsActivity.TAG, "Point of interest successfully removed")
                }

                alertDialog.dismiss()
            }
        }
    })
    alertDialog.dismiss()

    /*
    for (i in myList.keys()){
        if(toRemove == myList.getJSONObject(i).get("name") as String) {

            val mark = mymarker[i] as Marker
            val removed = myList.getJSONObject(i)
            mark.remove()
            mymarker.remove(i)
            myList.remove(i)
            val cancel = "Annulla"
            val text = "Rimosso $toRemove"
            val id = Auth.signInAccount?.email?.replace("@gmail.com","")!!
            // create a Toast to undo the operation of removing
            val snackbar = Snackbar.make(view, text, 5000)
                .setAction(cancel) {
//                    val (name,address,type,visibility,lat,lon,phone,url) = generateValue(removed)
                    val name = removed.get("name").toString()
                    val address = removed.get("address").toString()
                    val type = removed.get("type").toString()
                    val visibility =  removed.get("visibility").toString()
                    val lat = removed.get("latitude") as Double
                    val lon = removed.get("longitude") as Double
                    val phone = removed.get("phoneNumber").toString()
                    val url = removed.get("url").toString()

                    val newJsonMark = createJsonMarker(name,address,type,visibility,lat,lon,phone,url,id)
                    myList.put(mark.position.toString(), newJsonMark)
                    mymarker.put(mark.position.toString(), mark)
                    Toast.makeText(
                        mapsActivityContext,
                        "Annullata rimozione di $toRemove",
                        Toast.LENGTH_LONG
                    ).show()

                    showPOI()
                }
            snackbar.setActionTextColor(Color.DKGRAY)
            val snackView = snackbar.view
            snackView.setBackgroundColor(Color.BLACK)
            snackbar.show()


            snackbar.addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                override fun onShown(transientBottomBar: Snackbar?) {
                    super.onShown(transientBottomBar)
                }
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    super.onDismissed(transientBottomBar, event)
                    //remove from db the poi
                    removePOI(removed.get("markId").toString(),id)
                }
            })
            alertDialog.dismiss()
            break
        }
    }
    */
}
