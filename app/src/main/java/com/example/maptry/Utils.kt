package com.example.maptry

import android.R
import android.app.*
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.startActivity
import com.example.maptry.MapsActivity.Companion.account
import com.example.maptry.MapsActivity.Companion.addrThread
import com.example.maptry.MapsActivity.Companion.alertDialog
import com.example.maptry.MapsActivity.Companion.friendTempPoi
import com.example.maptry.MapsActivity.Companion.geocoder
import com.example.maptry.MapsActivity.Companion.lastLocation
import com.example.maptry.MapsActivity.Companion.listAddr
import com.example.maptry.MapsActivity.Companion.mAnimation
import com.example.maptry.MapsActivity.Companion.mMap
import com.example.maptry.MapsActivity.Companion.myList
import com.example.maptry.MapsActivity.Companion.mymarker
import com.example.maptry.MapsActivity.Companion.oldPos
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.DataSnapshot
import org.json.JSONObject
import java.io.IOException
import java.lang.Exception


var jsonNotifId = JSONObject()
var notificationJson = JSONObject()
var newFriendJson = JSONObject()


// re draw all poi
fun reDraw(){
    mMap.clear()
    var tmp = mymarker
    mymarker = JSONObject()
    for(i in tmp.keys()){
        //control in myList for color
        val mark : Marker = tmp[i] as Marker
        val marker = createMarker(mark.position)
        try{
            val cont = myList.getJSONObject(i).get("cont")
            println(cont)
            if(cont == "Live") marker?.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            else if(cont == "Macchina") marker?.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
            else marker?.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        }
        catch(e: Exception){
            println("some error")
            println(e)
        }
    }
    tmp = JSONObject()
    try{
        val x = LatLng(lastLocation.latitude, lastLocation.longitude)
        oldPos?.remove()
        oldPos = createMarker(x)
    }
    catch (e:Exception){
        println("first time")
    }

    mymarker.remove(oldPos?.position.toString())
}

// move Frame of actuvuty_maps layout
fun switchFrame(toView: FrameLayout, toGone1: FrameLayout, toGone2: FrameLayout, toGone3: FrameLayout, toGone4: FrameLayout,toGone5: FrameLayout,toGone6: FrameLayout,toGone7: FrameLayout,toGone8: FrameLayout){
    toGone1.invalidate()
    toGone2.invalidate()
    toGone3.invalidate()
    toGone4.invalidate()
    toGone5.invalidate()
    toGone6.invalidate()
    toGone7.invalidate()
    toGone8.invalidate()

    toView.visibility = View.VISIBLE
    toGone1.visibility = View.GONE
    toGone2.visibility = View.GONE
    toGone3.visibility = View.GONE
    toGone4.visibility = View.GONE
    toGone5.visibility = View.GONE
    toGone6.visibility = View.GONE
    toGone7.visibility = View.GONE
    toGone8.visibility = View.GONE

    toView.startAnimation(mAnimation)
    mAnimation.start()
    toView.bringToFront()
}

// simply create a marker, return it and add it to mymarker
fun createMarker(p0: LatLng): Marker? {

    var background = object : Runnable {
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


    var text = "Indirizzo:" + listAddr?.get(0)?.getAddressLine(0)+"\nGeoLocalita:" +  listAddr?.get(0)?.getLocality() + "\nAdminArea: " + listAddr?.get(0)?.getAdminArea() + "\nCountryName: " + listAddr?.get(0)?.getCountryName()+ "\nPostalCode: " + listAddr?.get(0)?.getPostalCode() + "\nFeatureName: " + listAddr?.get(0)?.getFeatureName();

    var x = mMap.addMarker(
        MarkerOptions()
            .position(p0)
            .title(text)
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            .alpha(0.7f)
    )

    mymarker.put(p0.toString(),x)
    return x
}

// show a dialog with infromation of friend's poi selected, can be add to user's poi
fun showPOIPreferences(p0 : String,inflater:LayoutInflater,context:Context,mark:Marker){

    val dialogView: View = inflater.inflate(com.example.maptry.R.layout.dialog_custom_friend_poi, null)
    var added = 0
    val address: TextView = dialogView.findViewById(com.example.maptry.R.id.txt_addressattr)
    val phone: TextView = dialogView.findViewById(com.example.maptry.R.id.phone_contentattr)
    val header: TextView = dialogView.findViewById(com.example.maptry.R.id.headerattr)
    val url: TextView = dialogView.findViewById(com.example.maptry.R.id.uri_lblattr)
    val text : String =  friendTempPoi.getJSONObject(p0).get("type") as String+": "+ friendTempPoi.getJSONObject(p0).get("name") as String
    header.text =  text
    address.text = friendTempPoi.getJSONObject(p0).get("addr") as String
    url.text = friendTempPoi.getJSONObject(p0).get("url") as String
    phone.text = friendTempPoi.getJSONObject(p0).get("phone") as String
    val routebutton: Button = dialogView.findViewById(com.example.maptry.R.id.routeBtn)
    val addbutton: Button = dialogView.findViewById(com.example.maptry.R.id.removeBtnattr)
    val id = account?.email?.replace("@gmail.com","")
    addbutton.text = "Aggiungi"
    addbutton.setOnClickListener {

        var mark = mymarker.get(p0 as String) as Marker
        myList.put(p0,friendTempPoi.getJSONObject(p0))

        added = 1
        val cont = friendTempPoi.getJSONObject(p0.toString()).get("cont")
        mark.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        alertDialog.dismiss()
    }
    routebutton.setOnClickListener {
        var intent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + address.text))
        added = 0
        startActivity(context,intent,null)
        alertDialog.dismiss()
    }

    val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(context)
    dialogBuilder.setOnDismissListener(object : DialogInterface.OnDismissListener {
        override fun onDismiss(arg0: DialogInterface) {
            if(added == 0) {
                mymarker.remove(p0)
                mark.remove()
            }
            else writeNewPOI(account?.email?.replace("@gmail.com","") as String, friendTempPoi.getJSONObject(p0).get("name") as String,friendTempPoi.getJSONObject(p0).get("addr") as String,friendTempPoi.getJSONObject(p0).get("cont") as String,friendTempPoi.getJSONObject(p0).get("type") as String,mark,friendTempPoi.getJSONObject(p0).get("url") as String,friendTempPoi.getJSONObject(p0).get("phone") as String)
        }
    })
    dialogBuilder.setView(dialogView)

    alertDialog = dialogBuilder.create();
    alertDialog.show()
}


