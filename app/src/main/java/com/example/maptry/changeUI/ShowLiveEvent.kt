@file:Suppress("DEPRECATED_IDENTITY_EQUALS")

package com.example.maptry.changeUI

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.maptry.activity.MapsActivity
import com.example.maptry.activity.MapsActivity.Companion.context
import com.example.maptry.activity.MapsActivity.Companion.geocoder
import com.example.maptry.activity.MapsActivity.Companion.isRunning
import com.example.maptry.activity.MapsActivity.Companion.mMap
import com.example.maptry.activity.MapsActivity.Companion.newBundy
import com.example.maptry.activity.MapsActivity.Companion.zoom

import com.example.maptry.activity.MapsActivity.Companion.carLayout
import com.example.maptry.activity.MapsActivity.Companion.drawerLayout
import com.example.maptry.activity.MapsActivity.Companion.friendFrame
import com.example.maptry.activity.MapsActivity.Companion.friendLayout
import com.example.maptry.activity.MapsActivity.Companion.homeLayout
import com.example.maptry.activity.MapsActivity.Companion.listLayout
import com.example.maptry.activity.MapsActivity.Companion.liveLayout
import com.example.maptry.activity.MapsActivity.Companion.splashLayout
import com.example.maptry.activity.MapsActivity.Companion.loginLayout

import com.example.maptry.R
import com.example.maptry.switchFrame
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng


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

        switchFrame(homeLayout,friendFrame,listLayout,carLayout,drawerLayout,splashLayout,friendLayout,liveLayout,loginLayout)
        if(!isRunning) {
            val main = Intent(context, MapsActivity::class.java)
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

