package com.example.maptry.utils

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat.startActivity
import com.example.maptry.R
import com.example.maptry.activity.MapsActivity
import com.example.maptry.activity.MapsActivity.Companion.account
import com.example.maptry.activity.MapsActivity.Companion.addrThread
import com.example.maptry.activity.MapsActivity.Companion.alertDialog
import com.example.maptry.activity.MapsActivity.Companion.drawerLayout
import com.example.maptry.activity.MapsActivity.Companion.friendFrame
import com.example.maptry.activity.MapsActivity.Companion.friendLayout
import com.example.maptry.activity.MapsActivity.Companion.friendTempPoi
import com.example.maptry.activity.MapsActivity.Companion.geocoder
import com.example.maptry.activity.MapsActivity.Companion.homeLayout
import com.example.maptry.activity.MapsActivity.Companion.lastLocation
import com.example.maptry.activity.MapsActivity.Companion.listAddr
import com.example.maptry.activity.MapsActivity.Companion.listLayout
import com.example.maptry.activity.MapsActivity.Companion.liveLayout
import com.example.maptry.activity.MapsActivity.Companion.mAnimation
import com.example.maptry.activity.MapsActivity.Companion.mMap
import com.example.maptry.activity.MapsActivity.Companion.myList
import com.example.maptry.activity.MapsActivity.Companion.mymarker
import com.example.maptry.activity.MapsActivity.Companion.oldPos
import com.example.maptry.activity.MapsActivity.Companion.splashLayout
import com.example.maptry.activity.MapsActivity.Companion.supportManager
import com.example.maptry.changeUI.CircleTransform
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.squareup.picasso.Picasso
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.lang.Exception
import java.lang.reflect.Array

var notificationJson = JSONObject()


// re draw all poi
fun reDraw(){
    mMap.clear()
    val tmp = mymarker
    mymarker = JSONObject()
    for(i in tmp.keys()){
        //control in myList for color
        val mark : Marker = tmp[i] as Marker
        val marker = createMarker(mark.position)
        try{
            val cont = myList.getJSONObject(i).get("type")
            println(cont)
            when (cont) {
                "Live" -> marker?.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                else -> marker?.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            }
        }
        catch(e: Exception){
            println("some error")
            println(e)
        }
    }
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



// move Frame of activity_maps layout
fun switchFrame(toView: FrameLayout, toHide: List<FrameLayout>) {
    toHide.forEach { frame ->
        frame.invalidate()
        frame.visibility = View.GONE
    }

    toView.visibility = View.VISIBLE
    toView.startAnimation(mAnimation)
    mAnimation.start()
    toView.bringToFront()
}

// simply create a marker, return it and add it to mymarker


// show a dialog with information of friend's poi selected, can be add to user's poi
@SuppressLint("SetTextI18n")
fun showPOIPreferences(p0 : String, inflater:LayoutInflater, context:Context, mark:Marker){

    val dialogView: View = inflater.inflate(R.layout.dialog_custom_friend_poi, null)
    var added = 0
    val address: TextView = dialogView.findViewById(R.id.txt_addressattr)
    val phone: TextView = dialogView.findViewById(R.id.phone_contentattr)
    val header: TextView = dialogView.findViewById(R.id.headerattr)
    val url: TextView = dialogView.findViewById(R.id.uri_lblattr)
    val text : String =  friendTempPoi.getJSONObject(p0).get("type") as String+": "+ friendTempPoi.getJSONObject(p0).get("name") as String
    header.text =  text
    address.text = friendTempPoi.getJSONObject(p0).get("address") as String
    url.text = friendTempPoi.getJSONObject(p0).get("url") as String
    phone.text = friendTempPoi.getJSONObject(p0).get("phoneNumber") as String
    val routebutton: Button = dialogView.findViewById(R.id.routeBtn)
    val addbutton: Button = dialogView.findViewById(R.id.removeBtnattr)
    addbutton.text = "Aggiungi"
    addbutton.setOnClickListener {

        val markAdd = mymarker.get(p0) as Marker
//        myList.put(p0,friendTempPoi.getJSONObject(p0))
        added = 1
        markAdd.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        alertDialog.dismiss()
    }
    routebutton.setOnClickListener {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + address.text))
        added = 0
        startActivity(context,intent,null)
        alertDialog.dismiss()
    }

    val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(context)
    dialogBuilder.setOnDismissListener {
        if (added == 0) {
            mymarker.remove(p0)
            mark.remove()
        } else {
            val name = friendTempPoi.getJSONObject(p0).get("name") as String
            val addr = friendTempPoi.getJSONObject(p0).get("address") as String
            val type = friendTempPoi.getJSONObject(p0).get("type") as String
            val visibility =  friendTempPoi.getJSONObject(p0).get("visibility") as String
            val lat = mark.position.latitude
            val lon = mark.position.longitude
            val phoneFriend = friendTempPoi.getJSONObject(p0).get("url") as String
            val urlFriend = friendTempPoi.getJSONObject(p0).get("phoneNumber") as String
            val id = account?.email?.replace("@gmail.com", "") as String
            val newJsonMark = createJsonMarker(name,addr,type,visibility,lat,lon,phoneFriend,urlFriend,id)
            myList.put(p0, newJsonMark)
        }

    }

    dialogBuilder.setView(dialogView)
    alertDialog = dialogBuilder.create()
    alertDialog.show()
}

fun setHomeLayout(navBar : View){
    println("IN SHOW HOME")
    val imageView = navBar.findViewById<ImageView>(R.id.imageView)
    val user = navBar.findViewById<TextView>(R.id.user)
    val email = navBar.findViewById<TextView>(R.id.email)
    val close = navBar.findViewById<ImageView>(R.id.close)
    val autoCompleteFragment =
        supportManager.findFragmentById(R.id.autocomplete_fragment) as? AutocompleteSupportFragment
    val layout: LinearLayout = autoCompleteFragment?.view as LinearLayout
    val menuIcon: ImageView = layout.getChildAt(0) as ImageView
    imageView.visibility = View.VISIBLE
    // load google photo
    Picasso.get().load(account?.photoUrl).into(imageView)
    Picasso.get()
        .load(account?.photoUrl)
        .transform(CircleTransform())
        .resize(100, 100)
        .into(menuIcon)
    // init menu
    menuIcon.setOnClickListener {
        switchFrame(
            drawerLayout,
            listOf(
                listLayout,
                homeLayout,
                friendLayout,
                friendFrame,
                splashLayout,
                liveLayout
            )
        )
    }
    user.visibility = View.VISIBLE
    user.text = account?.displayName
    email.visibility = View.VISIBLE
    email.text = account?.email
    close.visibility = View.VISIBLE
    println("fine SHOW HOME")

}

fun makeRedLine(lname: EditText,color: Int){
    lname.background.mutate().apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            colorFilter = BlendModeColorFilter(color, BlendMode.SRC_IN)
        }else{
            setColorFilter(color, PorterDuff.Mode.SRC_IN)
        }
    }
}

//fun generateValue(element: JSONObject): kotlin.Array<String> {
//    val name = element.get("name").toString()
//    val address = element.get("address").toString()
//    val type = element.get("type").toString()
//    val visibility =  element.get("visibility").toString()
//    val lat = element.get("latitude").toString()
//    val lon = element.get("longitude").toString()
//    val phone = element.get("phoneNumber").toString()
//    val url = element.get("url").toString()
//    return arrayOf(name, address, type, visibility, lat, lon, phone, url)
//}

fun toJsonObject(jsonArray: JSONArray):JSONObject{
    val toReturn = JSONObject()
    for (i in 0 until jsonArray.length()) {
        toReturn.put(i.toString(),jsonArray.getJSONObject(i))
    }
    return toReturn
}
