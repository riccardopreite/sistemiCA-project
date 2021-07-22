@file:Suppress("DEPRECATED_IDENTITY_EQUALS")

package com.example.maptry.changeUI

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.maptry.activity.MapsActivity
import com.example.maptry.activity.MapsActivity.Companion.context
import com.example.maptry.activity.MapsActivity.Companion.isRunning
import com.example.maptry.activity.MapsActivity.Companion.zoom

import com.example.maptry.activity.MapsActivity.Companion.drawerLayout
import com.example.maptry.activity.MapsActivity.Companion.friendFrame
import com.example.maptry.activity.MapsActivity.Companion.friendLayout
import com.example.maptry.activity.MapsActivity.Companion.homeLayout
import com.example.maptry.activity.MapsActivity.Companion.listLayout
import com.example.maptry.activity.MapsActivity.Companion.liveLayout
import com.example.maptry.activity.MapsActivity.Companion.splashLayout

import com.example.maptry.R
import com.example.maptry.activity.MapsActivity.Companion.account
import com.example.maptry.dataclass.ConfirmRequest
import com.example.maptry.dataclass.FriendRequest
import com.example.maptry.server.confirmFriend
import com.example.maptry.server.sendFriendRequest
import com.example.maptry.utils.reDraw
import com.example.maptry.utils.switchFrame

class ShowFriendRequest : AppCompatActivity() {
    @SuppressLint("SetTextI18n")

    // show a layout to accept/decline friend request
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

        println("entrato richiesta")
        switchFrame(friendFrame,listOf(drawerLayout,listLayout,homeLayout,splashLayout,friendLayout,liveLayout))
        val extras = intent?.extras
        val sender = extras?.get("sender") as String
        val receiver = extras.get("receiver") as String

        val buttonAccept:Button = findViewById(R.id.acceptFriendRequest)
        val friendTextView:TextView = findViewById(R.id.friendRequestText)
        friendTextView.text = "$sender ti ha inviato una richiesta di amicizia!"
        val buttonDecline:Button = findViewById(R.id.cancelFriendRequest)
        buttonAccept.setOnClickListener {
            val confirm = ConfirmRequest(receiver, sender)
            val jsonToAdd = gson.toJson(confirm)
            confirmFriend(jsonToAdd)
            switchFrame(homeLayout,listOf(drawerLayout,listLayout,friendFrame,friendLayout,splashLayout,liveLayout))
            if(!isRunning) {
                val main = Intent(context, MapsActivity::class.java)
                zoom = 1
                startActivity(main)
            }
            finish()
        }
        buttonDecline.setOnClickListener {
            switchFrame(homeLayout,listOf(drawerLayout,listLayout,friendFrame,friendLayout,splashLayout,liveLayout))
            if(!isRunning) {
                val main = Intent(context, MapsActivity::class.java)
                zoom = 1
                startActivity(main)
            }
            finish()
        }

    }
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        onSaveInstanceState(MapsActivity.newBundy)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBundle("newBundy", MapsActivity.newBundy)
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
            if(emailText.text.toString() !="" && emailText.text.toString() != "Inserisci Email" && emailText.text.toString() != account?.email && emailText.text.toString() != account?.email?.replace("@gmail.com","")){
                val id = account?.email?.replace("@gmail.com","")!!
                val sendRequest = FriendRequest(emailText.text.toString(),id)
                val jsonToAdd = gson.toJson(sendRequest)
                sendFriendRequest(jsonToAdd)

                MapsActivity.alertDialog.dismiss()
            }
        }
    }

}