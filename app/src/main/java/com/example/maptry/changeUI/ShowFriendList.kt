@file:Suppress("DEPRECATED_IDENTITY_EQUALS")

package com.example.maptry.changeUI

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.maptry.*
import com.example.maptry.activity.MapsActivity
import com.example.maptry.activity.MapsActivity.Companion.account
import com.example.maptry.activity.MapsActivity.Companion.alertDialog
import com.example.maptry.activity.MapsActivity.Companion.context
import com.example.maptry.activity.MapsActivity.Companion.isRunning
import com.example.maptry.activity.MapsActivity.Companion.zoom
import com.example.maptry.activity.MapsActivity.Companion.drawerLayout
import com.example.maptry.activity.MapsActivity.Companion.friendFrame
import com.example.maptry.activity.MapsActivity.Companion.friendJson
import com.example.maptry.activity.MapsActivity.Companion.friendLayout
import com.example.maptry.activity.MapsActivity.Companion.friendTempPoi
import com.example.maptry.activity.MapsActivity.Companion.homeLayout
import com.example.maptry.activity.MapsActivity.Companion.listLayout
import com.example.maptry.activity.MapsActivity.Companion.liveLayout
import com.example.maptry.activity.MapsActivity.Companion.splashLayout
import com.example.maptry.activity.MapsActivity.Companion.mMap
import com.example.maptry.activity.MapsActivity.Companion.newBundy
import com.example.maptry.dataclass.ConfirmRequest
import com.example.maptry.dataclass.FriendRequest
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.snackbar.Snackbar
import org.json.JSONException
import org.json.JSONObject
import com.example.maptry.utils.switchFrame
import com.example.maptry.server.confirmFriend
import com.example.maptry.server.getPoiFromFriend
import com.example.maptry.server.removeFriend
import com.example.maptry.server.sendFriendRequest
import com.example.maptry.utils.createMarker
import com.example.maptry.utils.showPOIPreferences


@SuppressLint("Registered")
class ShowFriendList : AppCompatActivity() {
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        println("inizio")
        setContentView(R.layout.activity_maps)

        drawerLayout = findViewById(R.id.drawer_layout)
        listLayout = findViewById(R.id.list_layout)
        homeLayout = findViewById(R.id.homeframe)
        splashLayout = findViewById(R.id.splashFrame)
        friendLayout = findViewById(R.id.friend_layout)
        friendFrame = findViewById(R.id.friendFrame)
        liveLayout = findViewById(R.id.live_layout)

        switchFrame(friendLayout,listOf(listLayout,homeLayout,drawerLayout,friendFrame,splashLayout,liveLayout))
        //create connection
        val closeDrawer :ImageView = findViewById(R.id.close_listfriend)
        val addfriend: ImageView = findViewById(R.id.add_listfriend)
        addfriend.setOnClickListener {
            val inflater: LayoutInflater = this.layoutInflater
            val dialogView: View = inflater.inflate(R.layout.add_friend, null)
            val emailText : EditText = dialogView.findViewById(R.id.friendEmail)
            val addBtn: Button = dialogView.findViewById(R.id.friendBtn)
            val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
            dialogBuilder.setOnDismissListener { }
            dialogBuilder.setView(dialogView)
            alertDialog = dialogBuilder.create()
            alertDialog.show()

            addBtn.setOnClickListener {
                if(emailText.text.toString() !="" && emailText.text.toString() != "Inserisci Email" && emailText.text.toString() != account?.email && emailText.text.toString() != account?.email?.replace("@gmail.com","")){
                    val id = account?.email?.replace("@gmail.com","")!!
                    val sendRequest = FriendRequest(emailText.text.toString(),id)
                    val jsonToAdd = gson.toJson(sendRequest)
                    sendFriendRequest(jsonToAdd)
                    alertDialog.dismiss()
                }
            }
        }

        closeDrawer.setOnClickListener {
            switchFrame(homeLayout,listOf(friendLayout,drawerLayout,listLayout,splashLayout,friendFrame,liveLayout))
            if(!isRunning) {
                println("STARTO ACTIVITY")
                val main = Intent(context, MapsActivity::class.java)
                zoom = 1

                startActivity(main)

            }
            finish()

        }

        showFriendActivity()
    }

    private fun showFriendActivity(){
        val len = friendJson.length()
        var index = 0
        val txt: TextView = findViewById(R.id.nofriend)
        println("mostro frame")
        switchFrame(friendLayout,listOf(listLayout,homeLayout,drawerLayout,friendFrame,splashLayout,liveLayout))


        val  lv: ListView = findViewById(R.id.fv)
        val friendList = MutableList(len) { "" }
        if(len == 0) txt.visibility = View.VISIBLE
        else txt.visibility = View.INVISIBLE
        for (i in friendJson.keys()){
            friendList[index] = friendJson[i] as String
            index++
        }

        val  arrayAdapter : ArrayAdapter<String> = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, friendList)
        lv.setOnItemLongClickListener { parent, view, position, _ -> //id

            val inflater: LayoutInflater = this.layoutInflater
            val dialogView: View = inflater.inflate(R.layout.dialog_custom_eliminate, null)
            val eliminateBtn: Button = dialogView.findViewById(R.id.eliminateBtn)
            eliminateBtn.setOnClickListener {

                val selectedItem = parent.getItemAtPosition(position) as String

                for(i in friendJson.keys()){
                    if(selectedItem == friendJson[i] as String) {
                        friendJson.remove(i)
                        val cancelString = "Annulla"
                        val text = "Rimosso $selectedItem"
                        val id = account?.email?.replace("@gmail.com","")
                        val snackbar = Snackbar.make(view, text, 2000)
                            .setAction(cancelString) {

                                id?.let { _ ->
                                    friendJson.put(i, selectedItem)
                                    val confirm = ConfirmRequest(id, selectedItem)
                                    val jsonToAdd = gson.toJson(confirm)
                                    confirmFriend(jsonToAdd)
                                    Toast.makeText(this, "undo$selectedItem", Toast.LENGTH_LONG)
                                        .show()
                                    showFriendActivity()

                                }
                            }

                        snackbar.setActionTextColor(Color.DKGRAY)
                        val snackbarView = snackbar.view
                        snackbarView.setBackgroundColor(Color.BLACK)
                        snackbar.show()
                        if (id != null) {
                            val remove = FriendRequest(id, selectedItem)
                            val jsonToRemove = gson.toJson(remove)
                            removeFriend(jsonToRemove)
                            showFriendActivity()
                            alertDialog.dismiss()
                            return@setOnClickListener
                        }
                    }
                }

            }
            val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
            dialogBuilder.setOnDismissListener { }
            dialogBuilder.setView(dialogView)

            alertDialog = dialogBuilder.create()
            alertDialog.show()


            return@setOnItemLongClickListener true
        }


        lv.setOnItemClickListener { parent, _, position, _ -> //view e id
            val inflater: LayoutInflater = this.layoutInflater
            val dialogView: View = inflater.inflate(R.layout.dialog_friend_view, null)
            val txtName :TextView = dialogView.findViewById(R.id.friendNameTxt)
            val spinner : Spinner = dialogView.findViewById(R.id.planets_spinner_POI)
            val selectedItem = parent.getItemAtPosition(position) as String

            val context = this
            txtName.text = selectedItem
            //try to replace with function call
            val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(context)
            dialogBuilder.setOnDismissListener { }
            dialogBuilder.setView(dialogView)

            val alertDialog2 = dialogBuilder.create()
            val result: JSONObject = getPoiFromFriend(selectedItem)
            this@ShowFriendList.runOnUiThread {
                try {
                    alertDialog2.show()
                    val length = result.length()
                    val markerList = MutableList(length + 1) { "" }
                    var indexMarker = 1
                    markerList[0] = ""
                    for (i in result.keys()) {
                        markerList[indexMarker] =
                            result.getJSONObject(i).get("name") as String
                        indexMarker++
                    }
                    val arrayAdapter2: ArrayAdapter<String> = ArrayAdapter<String>(
                        context,
                        R.layout.support_simple_spinner_dropdown_item, markerList
                    )
                    spinner.onItemSelectedListener =
                        object : AdapterView.OnItemSelectedListener {
                            override fun onNothingSelected(parent: AdapterView<*>?) {

                            }

                            override fun onItemSelected(
                                parent: AdapterView<*>?,
                                view: View?,
                                position: Int,
                                id: Long
                            ) {
                                if (parent?.getItemAtPosition(position) as String != "") {
                                    var key = ""
                                    val selectedMarker =
                                        parent.getItemAtPosition(position) as String
                                    var lat = 0.0
                                    var lon = 0.0
                                    for (i in result.keys()) {

                                        if (result.getJSONObject(i)
                                                .get("name") == selectedMarker
                                        ) {
                                            key = i
                                            lat = result.getJSONObject(i).get("latitude")
                                                .toString()
                                                .toDouble()
                                            lon = result.getJSONObject(i).get("longitude")
                                                .toString()
                                                .toDouble()
                                        }

                                    }

                                    val pos = LatLng(
                                        lat,
                                        lon
                                    )

                                    val mark = createMarker(pos)
                                    friendTempPoi.put(
                                        pos.toString(),
                                        result.getJSONObject(key)
                                    )
                                    mMap.moveCamera(
                                        CameraUpdateFactory.newLatLngZoom(
                                            LatLng(
                                                lat,
                                                lon
                                            ), 20F
                                        )
                                    )

                                    if (!isRunning) {
                                        val main = Intent(context, MapsActivity::class.java)
                                        zoom = 1
                                        startActivity(main)

                                    }
                                    switchFrame(
                                        homeLayout,
                                        listOf(friendLayout,
                                        listLayout,
                                        drawerLayout,
                                        friendFrame,
                                        splashLayout,
                                        liveLayout)
                                    )
                                    alertDialog2.dismiss()
                                    showPOIPreferences(
                                        pos.toString(),
                                        inflater,
                                        context,
                                        mark!!
                                    )
                                    finish()
                                }
                            }

                        }
                    spinner.adapter = arrayAdapter2

                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }


//            val url = URL("https://"+ip+port+"/getPoiFromFriend?"+ URLEncoder.encode("friend", "UTF-8") + "=" + URLEncoder.encode(selectedItem, "UTF-8"))
//
//            val client = OkHttpClient()
//
//
//            val request = Request.Builder()
//                .url(url)
//                .build()
//
//            client.newCall(request).enqueue(object : Callback {
//                override fun onFailure(call: okhttp3.Call, e: IOException) {
//                    println("something went wrong")
//                }
//
//                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
//                    println("ON RESPONSE")
//
//
//                }
//            })
        }
        lv.adapter = arrayAdapter
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
        println("CLOSING VIEW")
        switchFrame(homeLayout,listOf(drawerLayout,listLayout,splashLayout,friendLayout,friendFrame,liveLayout))
        if(!isRunning) {
            println("STARTO ACTIVITY friend list")
            val main = Intent(context, MapsActivity::class.java)
            zoom = 1
            startActivity(main)
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
        alertDialog = dialogBuilder.create()
        alertDialog.show()

        addBtn.setOnClickListener {
            if(emailText.text.toString() !="" && emailText.text.toString() != "Inserisci Email" && emailText.text.toString() != account?.email && emailText.text.toString() != account?.email?.replace("@gmail.com","")){
                val id = account?.email?.replace("@gmail.com","")!!
                val sendRequest = FriendRequest(emailText.text.toString(),id)
                val jsonToAdd = gson.toJson(sendRequest)
                sendFriendRequest(jsonToAdd)
                alertDialog.dismiss()
            }
        }
    }

}