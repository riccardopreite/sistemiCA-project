@file:Suppress("DEPRECATED_IDENTITY_EQUALS")

package com.example.maptry.changeUI

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.snackbar.Snackbar
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.URL
import java.net.URLEncoder
import com.example.maptry.activity.MapsActivity.Companion.context
import com.example.maptry.activity.MapsActivity.Companion.ip
import com.example.maptry.activity.MapsActivity.Companion.isRunning
import com.example.maptry.activity.MapsActivity.Companion.port
import com.example.maptry.activity.MapsActivity.Companion.zoom
import com.example.maptry.server.confirmFriend
import com.example.maptry.server.removeFriend
import com.example.maptry.server.sendFriendRequest


@SuppressLint("Registered")
class ShowFriendList : AppCompatActivity() {
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        //create connection
        val drawerLayout: FrameLayout = findViewById(R.id.drawer_layout)
        val listLayout: FrameLayout = findViewById(R.id.list_layout)
        val homeLayout: FrameLayout = findViewById(R.id.homeframe)
        val splashLayout: FrameLayout = findViewById(R.id.splashFrame)
        val listFriendLayout: FrameLayout = findViewById(R.id.friend_layout)
        val friendLayout: FrameLayout = findViewById(R.id.friendFrame)
        val carLayout: FrameLayout = findViewById(R.id.car_layout)
        var closeDrawer :ImageView = findViewById(R.id.close_listfriend)
        val liveLayout: FrameLayout = findViewById(R.id.live_layout)
        val loginLayout: FrameLayout = findViewById(R.id.login_layout)
        val addfriend: ImageView = findViewById(R.id.add_listfriend)
        addfriend.setOnClickListener {
            val inflater: LayoutInflater = this.layoutInflater
            val dialogView: View = inflater.inflate(R.layout.add_friend, null)
            val emailText : EditText = dialogView.findViewById(R.id.friendEmail)
            val addBtn: Button = dialogView.findViewById(R.id.friendBtn)
            val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
            dialogBuilder.setOnDismissListener(object : DialogInterface.OnDismissListener {
                override fun onDismiss(arg0: DialogInterface) { }
            })
            dialogBuilder.setView(dialogView)
            MapsActivity.alertDialog = dialogBuilder.create();
            MapsActivity.alertDialog.show()

            addBtn.setOnClickListener {
                if(emailText.text.toString() !="" && emailText.text.toString() != "Inserisci Email" && emailText.text.toString() != MapsActivity.account?.email && emailText.text.toString() != MapsActivity.account?.email?.replace("@gmail.com","")){
                    MapsActivity.account?.email?.replace("@gmail.com","")?.let { it1 -> sendFriendRequest(emailText.text.toString(),it1) }
                    MapsActivity.alertDialog.dismiss()
                }
            }
        }
        switchFrame(listFriendLayout,homeLayout,drawerLayout,listLayout,splashLayout,friendLayout,carLayout,liveLayout,loginLayout)

        closeDrawer.setOnClickListener {
            switchFrame(homeLayout,listFriendLayout,drawerLayout,listLayout,splashLayout,friendLayout,carLayout,liveLayout,loginLayout)
            if(!isRunning) {
                val main = Intent(context, MapsActivity::class.java)
                zoom = 1
                startActivity(main)

            }
            finish()

        }
        showFriendinActivity()
    }

    fun showFriendinActivity(){
        val len = MapsActivity.friendJson.length()
        var index = 0
        val txt: TextView = findViewById(R.id.nofriend)
        val drawerLayout: FrameLayout = findViewById(R.id.drawer_layout)
        val listLayout: FrameLayout = findViewById(R.id.list_layout)
        val homeLayout: FrameLayout = findViewById(R.id.homeframe)
        val splashLayout: FrameLayout = findViewById(R.id.splashFrame)
        val friendLayout: FrameLayout = findViewById(R.id.friend_layout)
        val friendRequestLayout: FrameLayout = findViewById(R.id.friendFrame)
        val carLayout: FrameLayout = findViewById(R.id.car_layout)
        val liveLayout: FrameLayout = findViewById(R.id.live_layout)
        val loginLayout: FrameLayout = findViewById(R.id.login_layout)
        switchFrame(friendLayout,listLayout,homeLayout,drawerLayout,friendRequestLayout,splashLayout,carLayout,liveLayout,loginLayout)


        var  lv: ListView = findViewById(R.id.fv)
        val friendList = MutableList(len) { "" }
        if(len == 0) txt.visibility = View.VISIBLE
        else txt.visibility = View.INVISIBLE
        for (i in MapsActivity.friendJson.keys()){
            friendList[index] = MapsActivity.friendJson[i] as String
            index++
        }

        var  arrayAdapter : ArrayAdapter<String> = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, friendList)
        lv.setOnItemLongClickListener { parent, view, position, _ -> //id

            val inflater: LayoutInflater = this.layoutInflater
            val dialogView: View = inflater.inflate(R.layout.dialog_custom_eliminate, null)
            val eliminateBtn: Button = dialogView.findViewById(R.id.eliminateBtn)
            eliminateBtn.setOnClickListener {

                val selectedItem = parent.getItemAtPosition(position) as String

                for(i in MapsActivity.friendJson.keys()){
                    if(selectedItem == MapsActivity.friendJson[i] as String) {
                        var removed = selectedItem
                        MapsActivity.friendJson.remove(i)
                        var key = i
                        var AC:String
                        AC = "Annulla"
                        var text = "Rimosso $selectedItem"
                        var id = MapsActivity.account?.email?.replace("@gmail.com","")
                        val snackbar = Snackbar.make(view, text, 2000)
                            .setAction(AC,View.OnClickListener {

                                id?.let { _ -> //
                                    MapsActivity.friendJson.put(key,removed)
                                    confirmFriend(id,removed)
                                    Toast.makeText(this, "undo$selectedItem", Toast.LENGTH_LONG).show()
                                    showFriendinActivity()

                                }
                            })

                        snackbar.setActionTextColor(Color.DKGRAY)
                        val snackbarView = snackbar.view
                        snackbarView.setBackgroundColor(Color.BLACK)
                        snackbar.show()
                        if (id != null) {
                            removeFriend(id,removed)
                            showFriendinActivity()
                            MapsActivity.alertDialog.dismiss()
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

            MapsActivity.alertDialog = dialogBuilder.create();
            MapsActivity.alertDialog.show()


            return@setOnItemLongClickListener true
        }


        lv.setOnItemClickListener { parent, _, position, _ -> //view e id
            val inflater: LayoutInflater = this.layoutInflater
            val dialogView: View = inflater.inflate(R.layout.dialog_friend_view, null)
            var txtName :TextView = dialogView.findViewById(R.id.friendNameTxt)
            var spinner : Spinner = dialogView.findViewById(R.id.planets_spinner_POI)
            val selectedItem = parent.getItemAtPosition(position) as String

            var context = this
            txtName.text = selectedItem
            var url = URL("http://"+ip+port+"/getPoiFromFriend?"+ URLEncoder.encode("friend", "UTF-8") + "=" + URLEncoder.encode(selectedItem, "UTF-8"))
            var result: JSONObject
            val client = OkHttpClient()
            val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(context)
            dialogBuilder.setOnDismissListener { }
//            dialogBuilder.setOnDismissListener(object : DialogInterface.OnDismissListener {
//                override fun onDismiss(arg0: DialogInterface) { }
//            })
            dialogBuilder.setView(dialogView)

            var alertDialog2 = dialogBuilder.create();

            val request = Request.Builder()
                .url(url)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    println("something went wrong")
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    println("ON RESPONSEEEEEEE")

                    this@ShowFriendList.runOnUiThread(Runnable {
                        try {
                            alertDialog2.show()
                            result = JSONObject(response.body()?.string()!!)
                            val length = result.length()
                            val markerList = MutableList<String>(length+1,{""})
                            var indexMarker = 1
                            markerList[0] = ""
                            for(i in result.keys()){
                                markerList[indexMarker] = result.getJSONObject(i).get("name") as String
                                indexMarker++
                            }
                            var arrayAdapter2: ArrayAdapter<String> = ArrayAdapter<String>(context,
                                R.layout.support_simple_spinner_dropdown_item,markerList)
                            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
                                override fun onNothingSelected(parent: AdapterView<*>?) {

                                }

                                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                                    if(parent?.getItemAtPosition(position) as String != ""){
                                        var key = ""
                                        val selectedMarker =
                                            parent.getItemAtPosition(position) as String
                                        var lat = 0.0
                                        var lon = 0.0
                                        for (i in result.keys()) {

                                            if (result.getJSONObject(i).get("name") == selectedMarker) {
                                                key = i
                                                lat = result.getJSONObject(i).get("lat").toString()
                                                    .toDouble()
                                                lon = result.getJSONObject(i).get("lon").toString()
                                                    .toDouble()
                                            }

                                        }

                                        var pos: LatLng = LatLng(
                                            lat,
                                            lon
                                        )

                                        var mark = createMarker(pos)
                                        MapsActivity.friendTempPoi.put(pos.toString(), result.getJSONObject(key))
                                        MapsActivity.mMap.moveCamera(
                                            CameraUpdateFactory.newLatLngZoom(
                                                LatLng(
                                                    lat,
                                                    lon
                                                ), 20F
                                            )
                                        )

                                        if(!isRunning) {
                                            val main = Intent(context, MapsActivity::class.java)
                                            zoom = 1
                                            startActivity(main)

                                        }
                                        switchFrame(homeLayout,friendLayout,listLayout,drawerLayout,friendRequestLayout,carLayout,splashLayout,liveLayout,loginLayout)
                                        alertDialog2.dismiss()
                                        showPOIPreferences(pos.toString(),inflater,context,mark!!)
                                        finish()
                                    }
                                }

                            }
                            spinner.adapter = arrayAdapter2;

                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    })
                }
            })
        }
        lv.adapter = arrayAdapter;
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