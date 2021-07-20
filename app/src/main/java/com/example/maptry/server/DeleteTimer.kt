package com.example.maptry.server

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.widget.*
import com.example.maptry.activity.MapsActivity
import androidx.appcompat.app.AppCompatActivity
import com.example.maptry.activity.MapsActivity.Companion.account
import com.example.maptry.activity.MapsActivity.Companion.context
import com.example.maptry.activity.MapsActivity.Companion.db
import com.example.maptry.activity.MapsActivity.Companion.geocoder
import com.example.maptry.activity.MapsActivity.Companion.isRunning
import com.example.maptry.activity.MapsActivity.Companion.listAddr
import com.example.maptry.activity.MapsActivity.Companion.myCar
import com.example.maptry.activity.MapsActivity.Companion.myList
import com.example.maptry.activity.MapsActivity.Companion.mymarker
import com.example.maptry.activity.MapsActivity.Companion.newBundy
import com.example.maptry.activity.MapsActivity.Companion.zoom
import com.example.maptry.notification.NotifyService.Companion.jsonNotifyIdExpired
import com.example.maptry.notification.NotifyService.Companion.jsonNotifyIdRemind
import com.example.maptry.utils.reDraw
import com.google.android.gms.maps.model.Marker
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
                            val nameDatabase = document.data["name"]
                            if(nameDatabase == name)  {
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
        val notificationId = jsonNotifyIdExpired.get(owner)
       try{
           val notificationId2 = jsonNotifyIdRemind.get(owner)
           notificationManager.cancel(notificationId2 as Int)

       }
       catch (e:Exception){

       }
        notificationManager.cancel(notificationId as Int)
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