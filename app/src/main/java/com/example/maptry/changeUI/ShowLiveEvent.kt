@file:Suppress("DEPRECATED_IDENTITY_EQUALS")

package com.example.maptry.changeUI

import android.app.AlertDialog
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.example.maptry.activity.MapsActivity
import com.example.maptry.activity.MapsActivity.Companion.context
import com.example.maptry.activity.MapsActivity.Companion.geocoder
import com.example.maptry.activity.MapsActivity.Companion.isRunning
import com.example.maptry.activity.MapsActivity.Companion.mMap
import com.example.maptry.activity.MapsActivity.Companion.newBundy
import com.example.maptry.activity.MapsActivity.Companion.zoom

import com.example.maptry.activity.MapsActivity.Companion.drawerLayout
import com.example.maptry.activity.MapsActivity.Companion.friendFrame
import com.example.maptry.activity.MapsActivity.Companion.friendLayout
import com.example.maptry.activity.MapsActivity.Companion.homeLayout
import com.example.maptry.activity.MapsActivity.Companion.listLayout
import com.example.maptry.activity.MapsActivity.Companion.liveLayout
import com.example.maptry.activity.MapsActivity.Companion.splashLayout

import com.example.maptry.R
import com.example.maptry.config.Auth
import com.example.maptry.dataclass.FriendRequest
import com.example.maptry.server.sendFriendRequest
import com.example.maptry.utils.reDraw
import com.example.maptry.utils.switchFrame
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

        drawerLayout = findViewById(R.id.drawer_layout)
        listLayout = findViewById(R.id.list_layout)
        homeLayout = findViewById(R.id.homeframe)
        splashLayout = findViewById(R.id.splashFrame)
        friendLayout = findViewById(R.id.friend_layout)
        friendFrame = findViewById(R.id.friendFrame)
        liveLayout = findViewById(R.id.live_layout)

        println("in live notify")
        val extras = intent.extras
        name = extras?.get("name") as String
        owner = extras.get("owner") as String
        timer = extras.get("timer") as String
        address = extras.get("address") as String
        val list = geocoder.getFromLocationName(address,1)
        val lat = list[0].latitude
        val lon = list[0].longitude
        val p0 = LatLng(lat,lon)



        switchFrame(homeLayout,listOf(friendFrame,listLayout,drawerLayout,splashLayout,friendLayout,liveLayout))
        mMap.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                p0, 20F
            )
        )
        if(!isRunning) {
            val main = Intent(context, MapsActivity::class.java)
            zoom = 1
            main.putExtra(
                "lat",
                lat
            )
            main.putExtra(
                "lon",
                lon
            )
            startActivity(main)
            println("ritornato da activity main")
            mMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    p0, 20F
                )
            )
        }

        finish()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        onSaveInstanceState(newBundy)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBundle("newBundy", newBundy)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        savedInstanceState.getBundle("newBundy")
    }

    fun closeDrawer(view: View) {

        println(view)
        if(drawerLayout.visibility == View.GONE) {
            switchFrame(drawerLayout,listOf(homeLayout,listLayout,splashLayout,friendLayout,friendFrame,liveLayout))
            finish()
        }
        else {
            reDraw()
            if(!isRunning) {
                println("STARTO ACTIVITY")
                val main = Intent(context, MapsActivity::class.java)
                zoom = 1
                startActivity(main)

            }
            switchFrame(homeLayout,listOf(drawerLayout,listLayout,splashLayout,friendLayout,friendFrame,liveLayout))
            finish()
        }
    }

    fun addFriend(view: View) {
        println(view)
        val inflater: LayoutInflater = this.layoutInflater
        val dialogView: View = inflater.inflate(R.layout.add_friend, null)
        val emailText : EditText = dialogView.findViewById(R.id.friendEmail)
        val addBtn: Button = dialogView.findViewById(R.id.friendBtn)
        val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
        dialogBuilder.setOnDismissListener { }
        dialogBuilder.setView(dialogView)
        MapsActivity.alertDialog = dialogBuilder.create()
        MapsActivity.alertDialog.show()

        addBtn.setOnClickListener {
            if(emailText.text.toString() !="" && emailText.text.toString() != "Inserisci Email" && emailText.text.toString() != Auth.signInAccount?.email && emailText.text.toString() != Auth.signInAccount?.email?.replace("@gmail.com","")){
                val id = Auth.signInAccount?.email?.replace("@gmail.com","")!!
                val receiver = emailText.text.toString().replace("@gmail.com","")
                val sendRequest = FriendRequest(receiver,id)
                val jsonToAdd = gson.toJson(sendRequest)
                sendFriendRequest(jsonToAdd)

                MapsActivity.alertDialog.dismiss()
            }
        }
    }



}

