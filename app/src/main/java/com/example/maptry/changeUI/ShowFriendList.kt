@file:Suppress("DEPRECATED_IDENTITY_EQUALS")

package com.example.maptry.changeUI

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.maptry.*
import com.example.maptry.activity.MapsActivity
import com.example.maptry.activity.MapsActivity.Companion.alertDialog
import com.example.maptry.activity.MapsActivity.Companion.mapsActivityContext
import com.example.maptry.activity.MapsActivity.Companion.isRunning
import com.example.maptry.activity.MapsActivity.Companion.zoom
import com.example.maptry.activity.MapsActivity.Companion.drawerLayout
import com.example.maptry.activity.MapsActivity.Companion.friendFrame
import com.example.maptry.activity.MapsActivity.Companion.friendLayout
import com.example.maptry.activity.MapsActivity.Companion.friendTempPoi
import com.example.maptry.activity.MapsActivity.Companion.homeLayout
import com.example.maptry.activity.MapsActivity.Companion.listLayout
import com.example.maptry.activity.MapsActivity.Companion.liveLayout
import com.example.maptry.activity.MapsActivity.Companion.splashLayout
import com.example.maptry.activity.MapsActivity.Companion.mMap
import com.example.maptry.activity.MapsActivity.Companion.newBundy
import com.example.maptry.api.RetrofitInstances
import com.example.maptry.config.Auth
import com.example.maptry.dataclass.ConfirmRequest
import com.example.maptry.dataclass.FriendRequest
import com.example.maptry.model.friends.RemoveFriendshipRequest
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
import com.google.android.material.snackbar.BaseTransientBottomBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException


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
                if(emailText.text.toString() !="" && emailText.text.toString() != "Inserisci Email" && emailText.text.toString() != Auth.signInAccount?.email && emailText.text.toString() != Auth.signInAccount?.email?.replace("@gmail.com","")){
                    val id = Auth.signInAccount?.email?.replace("@gmail.com","")!!
                    val receiver = emailText.text.toString().replace("@gmail.com","")
                    val sendRequest = FriendRequest(receiver,id)
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
                val main = Intent(mapsActivityContext, MapsActivity::class.java)
                zoom = 1

                startActivity(main)

            }
            finish()

        }

        showFriendActivity()
    }

    private fun showFriendActivity(){
        val txt: TextView = findViewById(R.id.nofriend)
        switchFrame(friendLayout,listOf(homeLayout,drawerLayout,friendLayout,friendFrame,splashLayout,liveLayout))

        val lv:ListView = findViewById(R.id.fv)

        txt.visibility = if(MapsActivity.friendsList.size == 0) View.VISIBLE else View.INVISIBLE

        val arrayAdapter : ArrayAdapter<String> = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, MapsActivity.friendsList.map { it.friendUsername })

        lv.setOnItemLongClickListener { parent, view, position, _ -> // _ refers to an id
            val inflater: LayoutInflater = this.layoutInflater
            val dialogView: View = inflater.inflate(R.layout.dialog_custom_eliminate, null)
            val eliminateBtn: Button = dialogView.findViewById(R.id.eliminateBtn)

            eliminateBtn.setOnClickListener {
                val toRemove = parent.getItemAtPosition(position) as String
                // poisToRemove is a list in this code but it can actually only be of size 0 or 1 because of how the api are implemented.
                val friendsToRemove = MapsActivity.friendsList.filter { friend -> friend.friendUsername == toRemove }
                if(friendsToRemove.isEmpty()) {
                    return@setOnClickListener
                }
                var willDeleteFriend = true
                val friendToRemove = friendsToRemove.first()
                MapsActivity.friendsList.remove(friendToRemove)
                val userId = Auth.signInAccount?.email?.replace("@gmail.com","")!!

                val snackbar = Snackbar.make(view, R.string.removed_friend, 5000)
                    .setAction(R.string.cancel) {
                        willDeleteFriend = false
                        MapsActivity.friendsList.add(friendToRemove)
                        Toast.makeText(
                            mapsActivityContext,
                            view.resources.getString(R.string.canceled_removal),
                            Toast.LENGTH_LONG
                        ).show()
//                        showFriend()
                    }

                snackbar.addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                    override fun onShown(transientBottomBar: Snackbar?) {
                        super.onShown(transientBottomBar)
                    }
                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                        super.onDismissed(transientBottomBar, event)
                        if(!willDeleteFriend) {
                            return
                        }
                        CoroutineScope(Dispatchers.IO).launch {
                            val response = try {
                                RetrofitInstances.friendsApi.removeFriend(
                                    RemoveFriendshipRequest(friendToRemove.friendUsername, userId)
                                )
                            } catch (e: IOException) {
                                e?.message?.let { it1 -> Log.e(MapsActivity.TAG, it1) }
                                return@launch
                            } catch (e: HttpException) {
                                e?.message?.let { it1 -> Log.e(MapsActivity.TAG, it1) }
                                return@launch
                            }

                            if(response.isSuccessful) {
                                Log.i(MapsActivity.TAG, "Friend successfully removed")
                            }

                            alertDialog.dismiss()
                        }
                    }
                })

                snackbar.setActionTextColor(Color.DKGRAY)
                val snackView = snackbar.view
                snackView.setBackgroundColor(Color.BLACK)
                snackbar.show()

                alertDialog.dismiss()
            }

            val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
            dialogBuilder.setView(dialogView)

            alertDialog = dialogBuilder.create()
            alertDialog.show()

            return@setOnItemLongClickListener true
        }
        lv.setOnItemClickListener { parent, _, position, _ -> //view e id
            val inflater: LayoutInflater = this.layoutInflater
            val dialogView: View = inflater.inflate(R.layout.dialog_friend_view, null)
            val txtName :TextView = dialogView.findViewById(R.id.friendNameTxt)
            val spinner :Spinner = dialogView.findViewById(R.id.planets_spinner_POI)
            val selectedItem = parent.getItemAtPosition(position) as String

            val context = this
            txtName.text = selectedItem
            // ask public friend's poi with a server call
            val userId = Auth.signInAccount?.email?.replace("@gmail.com", "")!!
            CoroutineScope(Dispatchers.IO).launch {
                val response = try {
                    RetrofitInstances.pointOfInterestsApi.getPointsOfInterest(userId, selectedItem)
                } catch (e: IOException) {
                    e?.message?.let { it1 -> Log.e(MapsActivity.TAG, it1) }
                    return@launch
                } catch (e: HttpException) {
                    e?.message?.let { it1 -> Log.e(MapsActivity.TAG, it1) }
                    return@launch
                }

                if(response.isSuccessful && response.body() != null) {
                    val friendsPois = response.body()!!

                    CoroutineScope(Dispatchers.Main).launch {
                        val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(context)
                        dialogBuilder.setView(dialogView)
                        val alertDialog2 = dialogBuilder.create()
                        alertDialog2.show()

                        val arrayAdapter2: ArrayAdapter<String> = ArrayAdapter<String>(
                            context,
                            R.layout.support_simple_spinner_dropdown_item,
                            friendsPois.map { it.name }.plus("")
                        )

                        spinner.onItemSelectedListener =
                            object : AdapterView.OnItemSelectedListener {
                                override fun onNothingSelected(parent: AdapterView<*>?) {}
                                override fun onItemSelected(
                                    parent: AdapterView<*>?,
                                    view: View?,
                                    position: Int,
                                    id: Long
                                ) {
                                    val selectedItem = parent?.getItemAtPosition(position) as String
                                    if (selectedItem == "") {
                                        return
                                    }

                                    val selectedPoi = friendsPois.first { it.name == selectedItem }

                                    val pos = LatLng(selectedPoi.latitude, selectedPoi.longitude)
                                    val marker = createMarker(pos)
                                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 20F))
                                    switchFrame(
                                        homeLayout,
                                        listOf(
                                            friendLayout,
                                            listLayout,
                                            drawerLayout,
                                            friendFrame,
                                            splashLayout,
                                            liveLayout
                                        )
                                    )
                                    alertDialog2.dismiss()
                                    showPOIPreferences(pos.toString(), inflater, context, marker!!)
                                }
                            }
                        spinner.adapter = arrayAdapter2
                    }
                }
            }
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
            val main = Intent(mapsActivityContext, MapsActivity::class.java)
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
            if(emailText.text.toString() !="" && emailText.text.toString() != "Inserisci Email" && emailText.text.toString() != Auth.signInAccount?.email && emailText.text.toString() != Auth.signInAccount?.email?.replace("@gmail.com","")){
                val id = Auth.signInAccount?.email?.replace("@gmail.com","")!!
                val receiver = emailText.text.toString().replace("@gmail.com","")
                val sendRequest = FriendRequest(receiver,id)
                val jsonToAdd = gson.toJson(sendRequest)
                sendFriendRequest(jsonToAdd)
                alertDialog.dismiss()
            }
        }
    }

}