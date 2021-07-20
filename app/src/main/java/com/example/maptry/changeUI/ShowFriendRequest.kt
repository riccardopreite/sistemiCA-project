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
        carLayout = findViewById(R.id.car_layout)
        liveLayout = findViewById(R.id.live_layout)
        loginLayout = findViewById(R.id.login_layout)

        println("entrato richiesta")
        switchFrame(friendFrame,drawerLayout,listLayout,homeLayout,splashLayout,friendLayout,carLayout,liveLayout,loginLayout)
        val extras = intent?.extras
        val sender = extras?.get("sender") as String
        val receiver = extras.get("receiver") as String

        val buttonAccept:Button = findViewById(R.id.acceptFriendRequest)
        val friendTextView:TextView = findViewById(R.id.friendRequestText)
        friendTextView.text = "$sender ti ha inviato una richiesta di amicizia!"
        val buttonDecline:Button = findViewById(R.id.cancelFriendRequest)
        buttonAccept.setOnClickListener {
            confirmFriend(sender,receiver)
            switchFrame(homeLayout,drawerLayout,listLayout,friendFrame,friendLayout,splashLayout,carLayout,liveLayout,loginLayout)
            if(!isRunning) {
                val main = Intent(context, MapsActivity::class.java)
                zoom = 1
                startActivity(main)
            }
            finish()
        }
        buttonDecline.setOnClickListener {
            switchFrame(homeLayout,drawerLayout,listLayout,friendFrame,friendLayout,splashLayout,carLayout,liveLayout,loginLayout)
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

    fun closeDrawer(view: View) {

        println(view)
        if(drawerLayout.visibility == View.GONE) {
            switchFrame(drawerLayout,homeLayout,listLayout,splashLayout,friendLayout,friendFrame,carLayout,liveLayout,loginLayout)
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
            switchFrame(homeLayout,drawerLayout,listLayout,splashLayout,friendLayout,friendFrame,carLayout,liveLayout,loginLayout)
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
            if(emailText.text.toString() !="" && emailText.text.toString() != "Inserisci Email" && emailText.text.toString() != MapsActivity.account?.email && emailText.text.toString() != MapsActivity.account?.email?.replace("@gmail.com","")){
                MapsActivity.account?.email?.replace("@gmail.com","")?.let { it1 -> sendFriendRequest(emailText.text.toString(),it1) }
                MapsActivity.alertDialog.dismiss()
            }
        }
    }

}