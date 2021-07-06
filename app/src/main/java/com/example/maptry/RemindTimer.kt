package com.example.maptry

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.maptry.MapsActivity
import com.example.maptry.MapsActivity.Companion.account
import com.example.maptry.MapsActivity.Companion.alertDialog
import com.example.maptry.MapsActivity.Companion.context
import com.example.maptry.MapsActivity.Companion.isRunning
import com.example.maptry.MapsActivity.Companion.myCar
import com.example.maptry.MapsActivity.Companion.newBundy
import com.example.maptry.MapsActivity.Companion.zoom
import com.example.maptry.NotifyService.Companion.jsonNotifIdRemind


@Suppress("DEPRECATED_IDENTITY_EQUALS")
class RemindTimer : AppCompatActivity() {
    var name = ""
    var owner = ""
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var notificationManager : NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        setContentView(R.layout.activity_maps)
        val extras = intent?.extras
        name = extras?.get("name") as String
        owner = extras.get("owner") as String

        val close = findViewById<ImageView>(R.id.close_car)
        close.setOnClickListener {
            val drawerLayout: FrameLayout = findViewById(R.id.drawer_layout)
            val listLayout: FrameLayout = findViewById(R.id.list_layout)
            val homeLayout: FrameLayout = findViewById(R.id.homeframe)
            val splashLayout: FrameLayout = findViewById(R.id.splashFrame)
            val listFriendLayout: FrameLayout = findViewById(R.id.friend_layout)
            val friendLayout: FrameLayout = findViewById(R.id.friendFrame)
            val carLayout: FrameLayout = findViewById(R.id.car_layout)
            val liveLayout: FrameLayout = findViewById(R.id.live_layout)
            val loginLayout: FrameLayout = findViewById(R.id.login_layout)

            switchFrame(homeLayout,friendLayout,drawerLayout,listLayout,splashLayout,listFriendLayout,carLayout,liveLayout,loginLayout)

        }
        val notificaionId = jsonNotifIdRemind.get(owner)
        notificationManager.cancel(notificaionId as Int);
        // if activity is not Running start it, else show a popup to remind timer
        if(!isRunning) {
            val main = Intent(context,MapsActivity::class.java)
            zoom = 1
            startActivity(main)
        }
        else showCar()

        finish()
    }
    private fun showCar(){

        val inflater: LayoutInflater = this.layoutInflater
        val dialogView: View = inflater.inflate(R.layout.dialog_car_view, null)

        val txtName :TextView = dialogView.findViewById(R.id.car_name_txt)
        val address : TextView = dialogView.findViewById(R.id.carAddressValue)
        val timer : TimePicker = dialogView.findViewById(R.id.timePickerView)
        val remindButton : Button = dialogView.findViewById(R.id.remindButton)
        var key = ""

        remindButton.setOnClickListener {
            myCar.getJSONObject(key).put("timer",timer.hour*60 + timer.minute)
            alertDialog.dismiss()
            resetTimerAuto(myCar.getJSONObject(key))

        }
        for (i in myCar.keys()){
            // check the item to create the dialog
            if(myCar.getJSONObject(i).get("name") as String == name){
                key = i
                txtName.text = name
                address.text = myCar.getJSONObject(i).get("addr") as String
                val time = (myCar.getJSONObject(i).get("timer").toString()).toInt()
                val hour = time/60
                val minute = time - hour*60
                timer.setIs24HourView(true)
                timer.hour = hour
                timer.minute = minute
                val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(context)
                dialogBuilder.setOnDismissListener(object : DialogInterface.OnDismissListener {
                    override fun onDismiss(arg0: DialogInterface) { }
                })
                dialogBuilder.setView(dialogView)
                try{
                    alertDialog.dismiss()
                }
                catch(e:Exception){
                    println("Exception")
                    println(e)

                }

                alertDialog = dialogBuilder.create();
                // need to be runned on UIThread
                runOnUiThread(Runnable {
                    alertDialog.show()
                })



            }
        }
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