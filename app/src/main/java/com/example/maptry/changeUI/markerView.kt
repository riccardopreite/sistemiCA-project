package com.example.maptry.changeUI

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat.startActivity
import com.example.maptry.R
import com.example.maptry.activity.MapsActivity
import com.example.maptry.activity.MapsActivity.Companion.addrThread
import com.example.maptry.activity.MapsActivity.Companion.alertDialog
import com.example.maptry.activity.MapsActivity.Companion.mapsActivityContext
import com.example.maptry.activity.MapsActivity.Companion.drawerLayout
import com.example.maptry.activity.MapsActivity.Companion.friendFrame
import com.example.maptry.activity.MapsActivity.Companion.friendLayout
import com.example.maptry.activity.MapsActivity.Companion.geocoder
import com.example.maptry.activity.MapsActivity.Companion.homeLayout
import com.example.maptry.activity.MapsActivity.Companion.listAddr
import com.example.maptry.activity.MapsActivity.Companion.listLayout
import com.example.maptry.activity.MapsActivity.Companion.liveEventsList
import com.example.maptry.activity.MapsActivity.Companion.liveLayout
import com.example.maptry.activity.MapsActivity.Companion.mMap
import com.example.maptry.activity.MapsActivity.Companion.myjson
import com.example.maptry.activity.MapsActivity.Companion.poisList
import com.example.maptry.activity.MapsActivity.Companion.splashLayout
import com.example.maptry.api.RetrofitInstances
import com.example.maptry.config.Auth
import com.example.maptry.model.liveevents.AddLiveEvent
import com.example.maptry.model.liveevents.LiveEvent
import com.example.maptry.model.pointofinterests.AddPointOfInterest
import com.example.maptry.model.pointofinterests.AddPointOfInterestPoi
import com.example.maptry.model.pointofinterests.PointOfInterest
import com.example.maptry.utils.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

val gson = Gson()


@SuppressLint("SetTextI18n", "InflateParams")
fun markerView(inflater: LayoutInflater, p0: Marker): View{
    val dialogView: View = inflater.inflate(R.layout.dialog_custom_view, null)
    val address: TextView = dialogView.findViewById(R.id.txt_addressattr)
    val phone: TextView = dialogView.findViewById(R.id.phone_contentattr)
    val phoneCap: TextView = dialogView.findViewById(R.id.phone_content)
    val header: TextView = dialogView.findViewById(R.id.headerattr)
    val url: TextView = dialogView.findViewById(R.id.uri_lblattr)
    val urlCap: TextView = dialogView.findViewById(R.id.uri_lbl)
    when {
        poisList.any { it.latitude == p0.position.latitude && it.longitude == p0.position.longitude } -> {
            val poi = poisList.first { it.latitude == p0.position.latitude && it.longitude == p0.position.longitude }
            header.text = poi.type +  ": "+ poi.name
            address.text = poi.address
            phone.text = poi.phoneNumber
            url.text = poi.url
            phoneCap.text = "Phone"
            urlCap.text = "Website"
        }
        liveEventsList.any { it.latitude == p0.position.latitude && it.longitude == p0.position.longitude } -> {
            val liveEvent = liveEventsList.first { it.latitude == p0.position.latitude && it.longitude == p0.position.longitude }
            header.text = "Live" + ": " + liveEvent.name
            address.text = liveEvent.address
            phoneCap.text = "Ending on"
            phone.text = LocalDateTime.ofEpochSecond(liveEvent.expirationDate.toLong(), 0, OffsetDateTime.now().offset).format(
                DateTimeFormatter.ISO_LOCAL_DATE_TIME
            )
            urlCap.text = "Owner"
        }
        else -> {}
    }

    val routebutton: Button = dialogView.findViewById(R.id.routeBtn)
    val routeBtn: Button = dialogView.findViewById(R.id.addToPoisBtnattr)
    routeBtn.setOnClickListener {
        val shareIntent = Intent()
        shareIntent.action = Intent.ACTION_SEND
        shareIntent.type="text/plain"
        val liveEvent = liveEventsList.first { it.latitude == p0.position.latitude && it.longitude == p0.position.longitude }
        shareIntent.putExtra(Intent.EXTRA_TEXT, "https://maps.google.com/?q="+ p0.position.latitude +","+ p0.position.longitude)
        val createdIntent = Intent.createChooser(shareIntent,"Stai condividendo "+ liveEvent.name)
        startActivity(mapsActivityContext,createdIntent,null)
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
            startActivity(mapsActivityContext,intent,null)
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
    val id = Auth.signInAccount?.email?.replace("@gmail.com", "")

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
        mapsActivityContext,
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
            if(type == "Live event"){
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
            makeRedLine(lname,red)
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
                spinner.selectedItem.toString() == "Live event" -> {
                    liveEventsList.forEach {
                        if(text == it.name) {
                            makeRedLine(lname,red)
                            return@setOnClickListener
                        }
                        if(address.text == it.address) {
                            makeRedLine(lname, red)
                            return@setOnClickListener
                        }
                    }
                    time = timePicker.hour * 60 + timePicker.minute
                    val marker = createMarker(p0)
                    marker?.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))


//                    val newLiveJsonMark = createLiveJsonMarker(text,address.text.toString(),time.toString(),id!!)

                    CoroutineScope(Dispatchers.IO).launch {
                        val response = try {
                            RetrofitInstances.liveEventsApi.addLiveEvent(
                                AddLiveEvent(
                                    time,
                                    id!!,
                                    text,
                                    address.text.toString(),
                                    p0.latitude,
                                    p0.longitude
                                )
                            )
                        } catch (e: IOException) {
                            e?.message?.let { it1 -> Log.e(MapsActivity.TAG, it1) }
                            return@launch
                        } catch (e: HttpException) {
                            e?.message?.let { it1 -> Log.e(MapsActivity.TAG, it1) }
                            return@launch
                        }

                        if(response.isSuccessful && response.body() != null) {
                            val date = LocalDateTime.now()
                            liveEventsList.add(LiveEvent(
                                "",
                                address.text.toString(),
                                p0.latitude,
                                p0.longitude,
                                text,
                                id,
                                date.plusMinutes(time.toLong()).toEpochSecond(
                                    OffsetDateTime.now().offset)
                            ))
                            Log.i(MapsActivity.TAG, "Point of interest successfully added")
                        } else {
                            Log.e(MapsActivity.TAG, "Point of interest not added")
                        }
                    }

//                    myLive.put(p0.toString(), newLiveJsonMark)
//                    myList.put(p0.toString(), newLiveJsonMark)

                }
                //                spinner on item selected
                else -> {
                    liveEventsList.forEach {
                        if(it.name == text) {
                            makeRedLine(lname,red)
                            return@setOnClickListener
                        }
                        if(address.text == it.address) {
                            makeRedLine(lname,red)
                            return@setOnClickListener
                        }
                    }
                    val marker = createMarker(p0)
                    marker?.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))

                    val url =
                        if(listAddr?.get(0)?.url === null || listAddr?.get(0)?.url === "" || listAddr?.get(0)?.url === " ") "Url non trovato"
                        else listAddr?.get(0)?.url!!
                    val phone =
                        if(listAddr?.get(0)?.phone === null|| listAddr?.get(0)?.phone === "" || listAddr?.get(0)?.phone === " ") "cellulare non trovato"
                        else listAddr?.get(0)?.phone!!

//                    val newJsonMark = createJsonMarker(text,address.text.toString(),spinner.selectedItem.toString(),gender,marker?.position?.latitude!!,
//                        marker.position.longitude,phone,url,id!!)

                    val latitude = marker?.position?.latitude!!
                    val longitude = marker?.position?.longitude!!

                    CoroutineScope(Dispatchers.IO).launch {
                        val response = try {
                            RetrofitInstances.pointOfInterestsApi.addPointOfInterest(
                                AddPointOfInterest(
                                    AddPointOfInterestPoi(
                                        address.text.toString(),
                                        spinner.selectedItem.toString(),
                                        latitude,
                                        longitude,
                                        text,
                                        phone,
                                        gender,
                                        url
                                    ),
                                    id!!
                                )
                            )
                        } catch (e: IOException) {
                            e.message?.let { it1 -> Log.e(MapsActivity.TAG, it1) }
                            return@launch
                        } catch (e: HttpException) {
                            e.message?.let { it1 -> Log.e(MapsActivity.TAG, it1) }
                            return@launch
                        }

                        if(response.isSuccessful && response.body() != null) {
                            poisList.add(PointOfInterest(
                                response.body()!!,
                                address.text.toString(),
                                spinner.selectedItem.toString(),
                                latitude,
                                longitude,
                                text,
                                phone,
                                gender,
                                url
                            ))
                            Log.i(MapsActivity.TAG, "Point of interest successfully added")
                        } else {
                            Log.e(MapsActivity.TAG, "Point of interest not added")
                        }
                    }
//                    myList.put(p0.toString(), newJsonMark)

                }
            }
            alertDialog.dismiss()
        }
    }
    return dialogView
}

