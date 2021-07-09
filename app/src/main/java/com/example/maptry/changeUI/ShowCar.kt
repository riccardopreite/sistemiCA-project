@file:Suppress("DEPRECATED_IDENTITY_EQUALS")

package com.example.maptry.changeUI
//DO SAME THING OF SHOW FRIEND REQUEST FOR SHOW CAR AND RIMANDA
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.maptry.activity.MapsActivity
import com.example.maptry.activity.MapsActivity.Companion.account
import com.example.maptry.activity.MapsActivity.Companion.alertDialog
import com.example.maptry.activity.MapsActivity.Companion.context
import com.example.maptry.activity.MapsActivity.Companion.isRunning
import com.example.maptry.activity.MapsActivity.Companion.myCar
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
import com.example.maptry.server.resetTimerAuto
import com.example.maptry.switchFrame
import com.google.android.material.snackbar.Snackbar

class ShowCar : AppCompatActivity() {
    @SuppressLint("SetTextI18n")
    var name = ""

    @RequiresApi(Build.VERSION_CODES.O)
    // this activity simply show car list
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        val extras = intent?.extras
        name = extras?.get("name") as String
        switchFrame(carLayout,friendFrame,listLayout,homeLayout,drawerLayout,splashLayout,friendLayout,liveLayout,loginLayout)

        var close = findViewById<ImageView>(R.id.close_car)
        close.setOnClickListener {

            switchFrame(homeLayout,carLayout,friendFrame,listLayout,drawerLayout,splashLayout,friendLayout,liveLayout,loginLayout)
            if(!isRunning) {
                val main = Intent(context, MapsActivity::class.java)
                zoom = 1
                startActivity(main)
            }
            finish()
        }

        showCar()



    }
    @SuppressLint("ShowToast")
    @RequiresApi(Build.VERSION_CODES.O)
    private fun showCar(){
        val len = myCar.length()
        var index = 0
        var indexFull = 0
        val txt: TextView = findViewById(R.id.nocar)
        switchFrame(carLayout,friendFrame,listLayout,homeLayout,drawerLayout,splashLayout,friendLayout,liveLayout,loginLayout)


        var  lv: ListView = findViewById<ListView>(R.id.lvCar)
        val carList = MutableList<String>(len,{""})
        val carListFull = MutableList<String>(len*10,{""})
        if(len == 0) txt.visibility = View.VISIBLE
        else txt.visibility = View.INVISIBLE
        for (i in myCar.keys()){
            carList[index] = myCar.getJSONObject(i).get("name") as String
            index++
            for (x in myCar.getJSONObject(i).keys()) {
                carListFull[indexFull] = myCar.getJSONObject(i).get("name") as String
                indexFull++

            }
        }
        println(carList)
        var pos = 0

        var  arrayAdapter : ArrayAdapter<String> = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, carList)
        lv.adapter = arrayAdapter;
        for (i in myCar.keys()) {

            if (myCar.getJSONObject(i).get("name") as String == name) {

                lv.itemsCanFocus = true
                lv.setSelection(pos)
                println("PORC")
                println(lv.getItemAtPosition(pos))
                println(lv.selectedView)
                lv.setItemChecked(pos,true)
                break
            }
            pos++
        }



        lv.setOnItemLongClickListener { parent, view, position, _ -> //id

            val inflater: LayoutInflater = this.layoutInflater
            val dialogView: View = inflater.inflate(R.layout.dialog_custom_eliminate, null)
            val eliminateBtn: Button = dialogView.findViewById(R.id.eliminateBtn)
            eliminateBtn.setOnClickListener {

                val selectedItem = parent.getItemAtPosition(position) as String

                for(i in myCar.keys()){
                    if(selectedItem == myCar.getJSONObject(i).get("name") as String) {
                        var removed = myCar.getJSONObject(i)
                        myCar.remove(i)
                        var key = i
                        var AC:String
                        AC = "Annulla"
                        var text = "Rimosso $selectedItem"
                        var id = account?.email?.replace("@gmail.com","")
                        val snackbar = Snackbar.make(view, text, 5000)
                            .setAction(AC) {

                                id?.let { _ -> // it1
                                    myCar.put(key, removed)
                                    Toast.makeText(
                                        this,
                                        "undo$selectedItem",
                                        Toast.LENGTH_LONG
                                    )
                                    showCar()

                                }
                            }

                        snackbar.setActionTextColor(Color.DKGRAY)
                        val snackbarView = snackbar.view
                        snackbarView.setBackgroundColor(Color.BLACK)
                        snackbar.show()
                        if (id != null) {
                            showCar()
                            alertDialog.dismiss()
                            return@setOnClickListener
                        }
                    }
                }

            }
            val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
            dialogBuilder.setOnDismissListener(object : DialogInterface.OnDismissListener {
                override fun onDismiss(arg0: DialogInterface) { }
            })
            dialogBuilder.setView(dialogView)

            alertDialog = dialogBuilder.create();
            alertDialog.show()


            return@setOnItemLongClickListener true
        }


        lv.setOnItemClickListener { parent, _, position, _ -> //view e id
            val inflater: LayoutInflater = this.layoutInflater
            val dialogView: View = inflater.inflate(R.layout.dialog_car_view, null)
            var txtName :TextView = dialogView.findViewById(R.id.car_name_txt)
            var address : TextView = dialogView.findViewById(R.id.carAddressValue)
            var timer : TimePicker = dialogView.findViewById(R.id.timePickerView)
            var remindButton : Button = dialogView.findViewById(R.id.remindButton)
            var key = ""
            val selectedItem = parent.getItemAtPosition(position) as String
            txtName.text = selectedItem
            for (i in myCar.keys()){
                if(myCar.getJSONObject(i).get("name") as String == selectedItem){
                    key = i
                    address.text = myCar.getJSONObject(i).get("addr") as String
                    var time = (myCar.getJSONObject(i).get("timer").toString()).toInt()
                    var hour = time/60
                    var minute = time - hour*60
                    timer.setIs24HourView(true)
                    timer.hour = hour
                    timer.minute = minute
                }
            }

            remindButton.setOnClickListener {
                myCar.getJSONObject(key).put("timer",timer.hour*60 + timer.minute)
                alertDialog.dismiss()
                resetTimerAuto(myCar.getJSONObject(key))

            }

            val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
            dialogBuilder.setOnDismissListener(object : DialogInterface.OnDismissListener {
                override fun onDismiss(arg0: DialogInterface) { }
            })
            dialogBuilder.setView(dialogView)

            alertDialog = dialogBuilder.create();
            alertDialog.show()
        }

    }
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation === Configuration.ORIENTATION_LANDSCAPE) {

            onSaveInstanceState(MapsActivity.newBundy)
        } else if (newConfig.orientation === Configuration.ORIENTATION_PORTRAIT) {

            onSaveInstanceState(MapsActivity.newBundy)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBundle("newBundy", MapsActivity.newBundy)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        savedInstanceState.getBundle("newBundy")
    }

}