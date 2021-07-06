package com.example.maptry

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.location.Address
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import com.example.maptry.MapsActivity
import androidx.appcompat.app.AppCompatActivity
import com.example.maptry.MapsActivity.Companion.account
import com.example.maptry.MapsActivity.Companion.alertDialog
import com.example.maptry.MapsActivity.Companion.context
import com.example.maptry.MapsActivity.Companion.db
import com.example.maptry.MapsActivity.Companion.geocoder
import com.example.maptry.MapsActivity.Companion.isRunning
import com.example.maptry.MapsActivity.Companion.listAddr
import com.example.maptry.MapsActivity.Companion.mMap
import com.example.maptry.MapsActivity.Companion.myCar
import com.example.maptry.MapsActivity.Companion.myList
import com.example.maptry.MapsActivity.Companion.mymarker
import com.example.maptry.MapsActivity.Companion.newBundy
import com.example.maptry.MapsActivity.Companion.zoom
import com.example.maptry.NotifyService.Companion.jsonNotifIdExpired
import com.example.maptry.NotifyService.Companion.jsonNotifIdRemind
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.material.snackbar.Snackbar
import java.io.IOException
import java.net.URL
import java.net.URLEncoder
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import java.lang.Exception

@Suppress("DEPRECATED_IDENTITY_EQUALS")
@SuppressLint("Registered")
class DeleteTimer : AppCompatActivity() {
    var name = ""
    var owner = ""
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val notificationManager : NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val extras = intent?.extras
        val id = account?.email?.replace("@gmail.com","")

        owner = extras?.get("owner") as String
        name = extras.get("name") as String

        listAddr = geocoder.getFromLocationName(extras.get("address") as String, 1)
        //val location = (listAddr)?.get(0);
        //val p0 = location?.latitude?.let { LatLng(it, location.longitude) }

        for (i in myCar.keys()){
            if(myCar.getJSONObject(i).get("name") as String == name){
                myCar.remove(i)
                myList.remove(i)
                val mark = mymarker[i] as Marker
                mark.remove()
                mymarker.remove(i)

                id?.let { it1 -> db.collection("user").document(it1).collection("car").get()
                    .addOnSuccessListener { result ->
                        for (document in result) {
                            val namedb = document.data["name"]
                            if(namedb == name)  {
                                db.document("user/"+id+"/car/"+document.id).delete()
                                return@addOnSuccessListener
                            }
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.d("FAIL", "Error getting documents: ", exception)
                    }
                }
                reDraw()
                break
            }
        }
        val notificaionId = jsonNotifIdExpired.get(owner)
       try{
           val notificaionId2 = jsonNotifIdRemind.get(owner)
           notificationManager.cancel(notificaionId2 as Int)

       }
       catch (e:Exception){

       }
        notificationManager.cancel(notificaionId as Int)
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