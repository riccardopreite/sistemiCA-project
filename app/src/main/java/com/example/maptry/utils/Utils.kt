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
import com.example.maptry.activity.MapsActivity.Companion.alertDialog
import com.example.maptry.activity.MapsActivity.Companion.drawerLayout
import com.example.maptry.activity.MapsActivity.Companion.friendFrame
import com.example.maptry.activity.MapsActivity.Companion.friendLayout
import com.example.maptry.activity.MapsActivity.Companion.friendPointOfInterest
import com.example.maptry.activity.MapsActivity.Companion.homeLayout
import com.example.maptry.activity.MapsActivity.Companion.lastLocation
import com.example.maptry.activity.MapsActivity.Companion.listLayout
import com.example.maptry.activity.MapsActivity.Companion.liveLayout
import com.example.maptry.activity.MapsActivity.Companion.mAnimation
import com.example.maptry.activity.MapsActivity.Companion.mMap
import com.example.maptry.activity.MapsActivity.Companion.myList
import com.example.maptry.activity.MapsActivity.Companion.mymarker
import com.example.maptry.activity.MapsActivity.Companion.oldPos
import com.example.maptry.activity.MapsActivity.Companion.splashLayout
import com.example.maptry.activity.MapsActivity.Companion.supportManager
import com.example.maptry.api.RetrofitInstances
import com.example.maptry.changeUI.CircleTransform
import com.example.maptry.config.Auth
import com.example.maptry.model.pointofinterests.AddPointOfInterest
import com.example.maptry.model.pointofinterests.AddPointOfInterestPoi
import com.example.maptry.model.pointofinterests.PointOfInterest
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException
import java.lang.Exception

var notificationJson = JSONObject()


// re draw all poi
fun reDraw(){
    Log.v("Utils", "reDraw")
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
    Log.v("Utils", "switchFrame")
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
    Log.v("Utils", "showPOIPreferences")
    val dialogView: View = inflater.inflate(R.layout.dialog_custom_friend_poi, null)
    var added = true
    val address: TextView = dialogView.findViewById(R.id.txt_addressattr)
    val phone: TextView = dialogView.findViewById(R.id.phone_contentattr)
    val header: TextView = dialogView.findViewById(R.id.headerattr)
    val url: TextView = dialogView.findViewById(R.id.uri_lblattr)
    friendPointOfInterest?.let {
        val text : String =  it.type + ": " + it.name
        header.text =  text
        address.text = it.address
        url.text = it.url
        phone.text = it.phoneNumber
    }

    val routebutton: Button = dialogView.findViewById(R.id.routeBtn)
    val addbutton: Button = dialogView.findViewById(R.id.removeBtnattr)
    addbutton.text = context.getString(R.string.add_to_user_pois)
    addbutton.setOnClickListener {
        val markAdd = mymarker.get(p0) as Marker
        added = true
        markAdd.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        alertDialog.dismiss()
    }
    routebutton.setOnClickListener {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + address.text))
        added = false
        startActivity(context,intent,null)
        alertDialog.dismiss()
    }

    val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(context)
    dialogBuilder.setOnDismissListener {
        if (!added) {
            mymarker.remove(p0)
            mark.remove()
        } else {
            val userId = Auth.signInAccount?.email?.replace("@gmail.com", "")!!
            CoroutineScope(Dispatchers.IO).launch {
                val response = try {
                    friendPointOfInterest?.let {
                        RetrofitInstances.pointOfInterestsApi.addPointOfInterest(
                            AddPointOfInterest(
                                userId,
                                AddPointOfInterestPoi(
                                    it.address,
                                    it.type,
                                    it.latitude,
                                    it.longitude,
                                    it.name,
                                    it.phoneNumber,
                                    it.visibility,
                                    it.url
                                )
                            )
                        )
                    }
                } catch (e: IOException) {
                    e?.message?.let { it1 -> Log.e(MapsActivity.TAG, it1) }
                    return@launch
                } catch (e: HttpException) {
                    e?.message?.let { it1 -> Log.e(MapsActivity.TAG, it1) }
                    return@launch
                }

                if(response?.isSuccessful == true && response?.body() != null) {
                    MapsActivity.poisList.add(
                        PointOfInterest(
                            response.body()!!,
                            friendPointOfInterest!!.address,
                            friendPointOfInterest!!.type,
                            friendPointOfInterest!!.latitude,
                            friendPointOfInterest!!.longitude,
                            friendPointOfInterest!!.name,
                            friendPointOfInterest!!.phoneNumber,
                            friendPointOfInterest!!.visibility,
                            friendPointOfInterest!!.url
                        )
                    )
                    Log.i(MapsActivity.TAG, "Point of interest originally of a friend successfully added")
                } else {
                    Log.e(MapsActivity.TAG, "Point of interest originally of a friend not added")
                }
            }
        }

    }

    dialogBuilder.setView(dialogView)
    alertDialog = dialogBuilder.create()
    alertDialog.show()
}

fun setHomeLayout(navBar : View){
    Log.v("Utils", "setHomeLayout")
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
    Picasso.get().load(Auth.signInAccount?.photoUrl).into(imageView)
    Picasso.get()
        .load(Auth.signInAccount?.photoUrl)
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
    user.text = Auth.signInAccount?.displayName
    email.visibility = View.VISIBLE
    email.text = Auth.signInAccount?.email
    close.visibility = View.VISIBLE
    println("fine SHOW HOME")

}

fun makeRedLine(lname: EditText,color: Int){
    Log.v("Utils", "makeRedLine")
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
    Log.v("Utils", "toJsonObject")
    val toReturn = JSONObject()
    for (i in 0 until jsonArray.length()) {
        toReturn.put(i.toString(),jsonArray.getJSONObject(i))
    }
    return toReturn
}
