package com.example.maptry.changeUI

import android.R.color
import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat.startActivity
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import com.example.maptry.R
import com.example.maptry.activity.MapsActivity.Companion.account
import com.example.maptry.activity.MapsActivity.Companion.addrThread
import com.example.maptry.activity.MapsActivity.Companion.alertDialog
import com.example.maptry.activity.MapsActivity.Companion.context
import com.example.maptry.activity.MapsActivity.Companion.drawerLayout
import com.example.maptry.activity.MapsActivity.Companion.friendFrame
import com.example.maptry.activity.MapsActivity.Companion.friendLayout
import com.example.maptry.activity.MapsActivity.Companion.geocoder
import com.example.maptry.activity.MapsActivity.Companion.homeLayout
import com.example.maptry.activity.MapsActivity.Companion.listAddr
import com.example.maptry.activity.MapsActivity.Companion.listLayout
import com.example.maptry.activity.MapsActivity.Companion.liveLayout
import com.example.maptry.activity.MapsActivity.Companion.mMap
import com.example.maptry.activity.MapsActivity.Companion.myList
import com.example.maptry.activity.MapsActivity.Companion.myLive
import com.example.maptry.activity.MapsActivity.Companion.myjson
import com.example.maptry.activity.MapsActivity.Companion.splashLayout
import com.example.maptry.server.startLive
import com.example.maptry.utils.createMarker
import com.example.maptry.utils.switchFrame
import com.example.maptry.utils.writeNewLive
import com.example.maptry.utils.writeNewPOI
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import org.json.JSONObject
import java.io.IOException


@SuppressLint("SetTextI18n", "InflateParams")
fun markerView(inflater: LayoutInflater, p0: Marker): View{
    val dialogView: View = inflater.inflate(R.layout.dialog_custom_view, null)
    val address: TextView = dialogView.findViewById(R.id.txt_addressattr)
    val phone: TextView = dialogView.findViewById(R.id.phone_contentattr)
    val phoneCap: TextView = dialogView.findViewById(R.id.phone_content)
    val header: TextView = dialogView.findViewById(R.id.headerattr)
    val url: TextView = dialogView.findViewById(R.id.uri_lblattr)
    val urlCap: TextView = dialogView.findViewById(R.id.uri_lbl)
    val text : String =  myList.getJSONObject(p0.position.toString()).get("cont") as String+": "+ myList.getJSONObject(p0.position.toString()).get("name") as String
    header.text =  text
    address.text = myList.getJSONObject(p0.position.toString()).get("addr") as String
    url.text = myList.getJSONObject(p0.position.toString()).get("url") as String
    phone.text = myList.getJSONObject(p0.position.toString()).get("phone") as String
    when {
        myList.getJSONObject(p0.position.toString()).get("cont") as String == "Live" -> {
            phone.text = myLive.getJSONObject(p0.position.toString()).get("timer") as String + " minuti"
            phoneCap.text = "Timer"
            url.text = myLive.getJSONObject(p0.position.toString()).get("owner") as String
            urlCap.text = "Proprietario"

        }
        else -> {
            phoneCap.text = "N.cellulare"
            urlCap.text = "WebSite"
        }
    }

    val routebutton: Button = dialogView.findViewById(R.id.routeBtn)
    val routeBtn: Button = dialogView.findViewById(R.id.removeBtnattr)
    routeBtn.setOnClickListener {
        val shareIntent = Intent()
        shareIntent.action = Intent.ACTION_SEND
        shareIntent.type="text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, "https://maps.google.com/?q="+ myList.getJSONObject(p0.position.toString()).get("lat")+","+ myList.getJSONObject(p0.position.toString()).get("lon"))
        val createdIntent = Intent.createChooser(shareIntent,"Stai condividendo "+ myList.getJSONObject(p0.position.toString()).get("name"))
        startActivity(context,createdIntent,null)
        alertDialog.dismiss()
    }

    if (homeLayout.visibility == View.GONE) {
        routebutton.text = "Visualizza"
        routebutton.setOnClickListener {
            mMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(
                        p0.position.latitude,
                        p0.position.longitude
                    ), 20F
                )
            )
            switchFrame(
                homeLayout,listOf(
                    listLayout,
                    drawerLayout,
                    friendLayout,
                    friendFrame,
                    splashLayout,
                    liveLayout
                ))
            alertDialog.dismiss()
        }
    }
    else{
        routebutton.setOnClickListener {
            val intent =
                Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + address.text))
            startActivity(context,intent,null)
            alertDialog.dismiss()
        }
    }
    return dialogView
}
@SuppressLint("InflateParams", "ResourceAsColor")
fun showCreateMarkerView(inflater: LayoutInflater, p0: LatLng): View{
    val dialogView: View = inflater.inflate(R.layout.dialog_list_view, null)
    val spinner: Spinner = dialogView.findViewById(R.id.planets_spinner)
    val lname : EditText = dialogView.findViewById(R.id.txt_lname)
    val address :  TextView = dialogView.findViewById(R.id.txt_address)
    val publicButton: RadioButton = dialogView.findViewById(R.id.rb_public)
    val privateButton: RadioButton = dialogView.findViewById(R.id.rb_private)
    val timePickerLayout = dialogView.findViewById<RelativeLayout>(R.id.timePicker)
    val timePicker = dialogView.findViewById<TimePicker>(R.id.timePicker1)
    val addbutton: Button = dialogView.findViewById(R.id.addBtn)
    val removebutton: Button = dialogView.findViewById(R.id.removeBtn)
    val id = account?.email?.replace("@gmail.com", "")

    timePicker.hour = 3
    timePicker.minute = 0
    val radioGroup = dialogView.findViewById<RelativeLayout>(R.id.rl_gender)
    var time : Int
    address.isEnabled = false

    val background = object : Runnable {
        override fun run() {
            try {
                listAddr = geocoder.getFromLocation(p0.latitude, p0.longitude, 1)
                return
            } catch (e: IOException) {
                Log.e("Error", "grpc failed: " + e.message, e)
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
    address.text = listAddr?.get(0)?.getAddressLine(0)

    ArrayAdapter.createFromResource(
        context,
        R.array.planets_array,
        android.R.layout.simple_spinner_item
    ).also { adapter ->
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }


    spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
        override fun onNothingSelected(parent: AdapterView<*>?) {

        }
        // show timepicker for live
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            val type = parent?.getItemAtPosition(position) as String
            if(type == "Live"){
                radioGroup.visibility = View.GONE
                timePicker.setIs24HourView(true)
                timePickerLayout.visibility = View.VISIBLE
            }
            else{
                radioGroup.visibility = View.VISIBLE
                timePicker.setIs24HourView(true)
                timePickerLayout.visibility = View.GONE
            }
        }
    }
    removebutton.setOnClickListener {
        alertDialog.dismiss()
    }
    addbutton.setOnClickListener {
//        val red = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(R.color.quantum_googred, BlendModeCompat.SRC_IN)
        val red = R.color.quantum_googred
        val text = lname.text.toString()
        if (text == "") {
            lname.background.mutate().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                    colorFilter = BlendModeColorFilter(red, BlendMode.SRC_IN)
                }else{
                    setColorFilter(red,PorterDuff.Mode.SRC_IN)
                }
            }
//            lname.background.mutate().colorFilter = red
        }
        else {
            myjson = JSONObject()
            var gender = "gen"
            if (publicButton.isChecked)
                gender = publicButton.text.toString()
            if (privateButton.isChecked)
                gender = privateButton.text.toString()

            when {
                spinner.selectedItem.toString() == "Live" -> {
                    for (i: String in myLive.keys()) {
                        try {
                            val x: String = myLive.getJSONObject(i).get("name") as String
                            if (text == x) {
                                lname.background.mutate().apply {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                                        colorFilter = BlendModeColorFilter(red, BlendMode.SRC_IN)
                                    }else{
                                        setColorFilter(red,PorterDuff.Mode.SRC_IN)
                                    }
                                }
                                return@setOnClickListener

                            }
                            if (address.text == myLive.getJSONObject(i).get("addr") as String) {
                                lname.background.mutate().apply {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                                        colorFilter = BlendModeColorFilter(red, BlendMode.SRC_IN)
                                    }else{
                                        setColorFilter(red,PorterDuff.Mode.SRC_IN)
                                    }
                                }
                                return@setOnClickListener
                            }
                        } catch (e: java.lang.Exception) {
                            println("ops")
                        }
                    }
                    time = timePicker.hour * 60 + timePicker.minute
                    val marker = createMarker(p0)
                    marker?.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))

                    myjson.put("type", "Pubblico")
                    myjson.put("timer", time.toString())
                    myjson.put("name", text)
                    myjson.put("addr", address.text.toString())
                    myjson.put("owner", account?.email?.replace("@gmail.com", ""))
                    myjson.put("marker", marker)
                    myjson.put("cont", spinner.selectedItem.toString())
                    myjson.put("url", "da implementare")
                    myjson.put("phone", "da implementare")
                    startLive(myjson)
                    myLive.put(p0.toString(), myjson)
                    myList.put(p0.toString(), myjson)

                    id?.let { it1 ->
                        if (marker != null) {
                            writeNewLive(
                                it1,
                                text,
                                address.text.toString(),
                                time.toString(),
                                it1,
                                marker,
                                "da implementare",
                                "da implementare",
                                "Pubblico",
                                "Live"
                            )
                        }
                    }
                }
                //                spinner on item selected
                else -> {
                    for (i: String in myList.keys()) {
                        try {
                            val x: String = myList.getJSONObject(i).get("name") as String
                            if (text == x) {
                                lname.background.mutate().apply {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                                        colorFilter = BlendModeColorFilter(red, BlendMode.SRC_IN)
                                    }else{
                                        setColorFilter(red,PorterDuff.Mode.SRC_IN)
                                    }
                                }
                                return@setOnClickListener

                            }
                            if (address.text == myList.getJSONObject(i).get("addr") as String) {
                                lname.background.mutate().apply {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                                        colorFilter = BlendModeColorFilter(red, BlendMode.SRC_IN)
                                    }else{
                                        setColorFilter(red,PorterDuff.Mode.SRC_IN)
                                    }
                                }
                                return@setOnClickListener
                            }
                        } catch (e: java.lang.Exception) {
                            println("ops")
                        }
                    }
                    val marker = createMarker(p0)
                    marker?.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))

                    myjson.put("name", text)
                    myjson.put("addr", address.text.toString())
                    myjson.put("cont", spinner.selectedItem.toString())
                    myjson.put("type", gender)
                    myjson.put("marker", marker)
                    myjson.put("url", "da implementare")
                    myjson.put("phone", "da implementare")
                    if(listAddr?.get(0)?.url === null || listAddr?.get(0)?.url === "" || listAddr?.get(0)?.url === " ") myjson.put("url","Url non trovato")
                    else  myjson.put("url", listAddr?.get(0)?.url)
                    if(listAddr?.get(0)?.phone === null|| listAddr?.get(0)?.phone === "" || listAddr?.get(0)?.phone === " ") myjson.put("phone","cellulare non trovato")
                    else  myjson.put("phone", listAddr?.get(0)?.phone)
                    myList.put(p0.toString(), myjson)
                    id?.let { it1 ->
                        if (marker != null) {
                            writeNewPOI(
                                it1,
                                text,
                                address.text.toString(),
                                spinner.selectedItem.toString(),
                                gender,
                                marker,
                                "da implementare",
                                "da implementare"
                            )
                        }
                    }
                }
            }
            alertDialog.dismiss()
        }
    }
    return dialogView
}