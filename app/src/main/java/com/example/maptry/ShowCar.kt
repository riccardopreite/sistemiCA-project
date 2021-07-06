@file:Suppress("DEPRECATED_IDENTITY_EQUALS")

package com.example.maptry
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
import androidx.core.view.get
import com.example.maptry.MapsActivity.Companion.account
import com.example.maptry.MapsActivity.Companion.alertDialog
import com.example.maptry.MapsActivity.Companion.context
import com.example.maptry.MapsActivity.Companion.isRunning
import com.example.maptry.MapsActivity.Companion.myCar
import com.example.maptry.MapsActivity.Companion.zoom
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
        //refactor to car layout
        val drawerLayout: FrameLayout = findViewById(R.id.drawer_layout)
        val listLayout: FrameLayout = findViewById(R.id.list_layout)
        val homeLayout: FrameLayout = findViewById(R.id.homeframe)
        val splashLayout: FrameLayout = findViewById(R.id.splashFrame)
        val friendLayout: FrameLayout = findViewById(R.id.friendFrame)
        val carLayout: FrameLayout = findViewById(R.id.car_layout)
        val friendRequestLayout: FrameLayout = findViewById(R.id.friend_layout)
        val liveLayout: FrameLayout = findViewById(R.id.live_layout)
        val loginLayout: FrameLayout = findViewById(R.id.login_layout)
        switchFrame(carLayout,friendLayout,listLayout,homeLayout,drawerLayout,splashLayout,friendRequestLayout,liveLayout,loginLayout)

        var close = findViewById<ImageView>(R.id.close_car)
        close.setOnClickListener {

            switchFrame(homeLayout,carLayout,friendLayout,listLayout,drawerLayout,splashLayout,friendRequestLayout,liveLayout,loginLayout)
            if(!isRunning) {
                val main = Intent(context,MapsActivity::class.java)
                zoom = 1
                startActivity(main)
            }
            finish()
        }

        showCar()



    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun showCar(){
        val len = myCar.length()
        var index = 0
        var indexFull = 0
        val txt: TextView = findViewById(R.id.nocar)

        val drawerLayout: FrameLayout = findViewById(R.id.drawer_layout)
        val listLayout: FrameLayout = findViewById(R.id.list_layout)
        val homeLayout: FrameLayout = findViewById(R.id.homeframe)
        val splashLayout: FrameLayout = findViewById(R.id.splashFrame)
        val friendLayout: FrameLayout = findViewById(R.id.friendFrame)
        val carLayout: FrameLayout = findViewById(R.id.car_layout)
        val friendRequestLayout: FrameLayout = findViewById(R.id.friend_layout)
        val liveLayout: FrameLayout = findViewById(R.id.live_layout)
        val loginLayout: FrameLayout = findViewById(R.id.login_layout)
        switchFrame(carLayout,friendLayout,listLayout,homeLayout,drawerLayout,splashLayout,friendRequestLayout,liveLayout,loginLayout)


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



        lv.setOnItemLongClickListener { parent, view, position, id ->

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
                        var text = "Rimosso "+selectedItem
                        var id = account?.email?.replace("@gmail.com","")
                        val snackbar = Snackbar.make(view, text, 5000)
                            .setAction(AC,View.OnClickListener {

                                id?.let { it1 ->
                                    myCar.put(key,removed)
                                    Toast.makeText(this,"undo" + selectedItem.toString(), Toast.LENGTH_LONG)
                                    showCar()

                                }
                            })

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


        lv.setOnItemClickListener { parent, view, position, id ->
            val inflater: LayoutInflater = this.layoutInflater
            val dialogView: View = inflater.inflate(R.layout.dialog_car_view, null)
            var txtName :TextView = dialogView.findViewById(R.id.car_name_txt)
            var address : TextView = dialogView.findViewById(R.id.carAddressValue)
            var timer : TimePicker = dialogView.findViewById(R.id.timePickerView)
            var remindButton : Button = dialogView.findViewById(R.id.remindButton)
            var key = ""
            val selectedItem = parent.getItemAtPosition(position) as String
            var context = this
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