@file:Suppress("DEPRECATED_IDENTITY_EQUALS")

package com.example.maptry

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.maptry.MapsActivity.Companion.account
import com.example.maptry.MapsActivity.Companion.alertDialog
import com.example.maptry.MapsActivity.Companion.context
import com.example.maptry.MapsActivity.Companion.dataFromfirestore
import com.example.maptry.MapsActivity.Companion.db
import com.example.maptry.MapsActivity.Companion.geocoder
import com.example.maptry.MapsActivity.Companion.isRunning
import com.example.maptry.MapsActivity.Companion.mMap
import com.example.maptry.MapsActivity.Companion.myList
import com.example.maptry.MapsActivity.Companion.myLive
import com.example.maptry.MapsActivity.Companion.myjson
import com.example.maptry.MapsActivity.Companion.mymarker
import com.example.maptry.MapsActivity.Companion.newBundy
import com.example.maptry.MapsActivity.Companion.zoom
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import org.json.JSONObject
import java.lang.Exception
import java.util.*
import kotlin.concurrent.schedule


class ShowLiveEvent: AppCompatActivity() {
    var name = ""
    var owner = ""
    var timer = ""
    var address = ""

    // move camera on live event
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        var extras = intent.extras
        name = extras?.get("name") as String
        owner = extras.get("owner") as String
        timer = extras.get("timer") as String
        address = extras.get("address") as String
        val list = geocoder.getFromLocationName(address,1)
        val lat = list[0].latitude
        val lon = list[0].longitude
        val p0 = LatLng(lat,lon)

        mMap.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                p0, 20F
            )
        )
        val drawerLayout: FrameLayout = findViewById(R.id.drawer_layout)
        val listLayout: FrameLayout = findViewById(R.id.list_layout)
        val homeLayout: FrameLayout = findViewById(R.id.homeframe)
        val splashLayout: FrameLayout = findViewById(R.id.splashFrame)
        val friendLayout: FrameLayout = findViewById(R.id.friendFrame)
        val carLayout: FrameLayout = findViewById(R.id.car_layout)
        val friendRequestLayout: FrameLayout = findViewById(R.id.friend_layout)
        val liveLayout: FrameLayout = findViewById(R.id.live_layout)
        val loginLayout: FrameLayout = findViewById(R.id.login_layout)
        switchFrame(homeLayout,friendLayout,listLayout,carLayout,drawerLayout,splashLayout,friendRequestLayout,liveLayout,loginLayout)
        if(!isRunning) {
            val main = Intent(context,MapsActivity::class.java)
            zoom = 1
            startActivity(main)

        }
        finish()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation === Configuration.ORIENTATION_LANDSCAPE) {

            onSaveInstanceState(newBundy)
        } else if (newConfig.orientation === Configuration.ORIENTATION_PORTRAIT) {

            onSaveInstanceState(newBundy)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBundle("newBundy", newBundy)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        savedInstanceState.getBundle("newBundy")
    }


}

