@file:Suppress("DEPRECATED_IDENTITY_EQUALS")

package com.example.maptry

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import com.example.maptry.MapsActivity
import androidx.appcompat.app.AppCompatActivity
import com.example.maptry.MapsActivity.Companion.context
import com.example.maptry.MapsActivity.Companion.isRunning
import com.example.maptry.MapsActivity.Companion.zoom
import java.io.IOException
import java.net.URL
import java.net.URLEncoder
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
class ShowFriendRequest : AppCompatActivity() {
    @SuppressLint("SetTextI18n")

    // show a layout to accept/decline friend request
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        val drawerLayout: FrameLayout = findViewById(R.id.drawer_layout)
        val carLayout: FrameLayout = findViewById(R.id.car_layout)

        val listLayout: FrameLayout = findViewById(R.id.list_layout)
        val homeLayout: FrameLayout = findViewById(R.id.homeframe)
        val splashLayout: FrameLayout = findViewById(R.id.splashFrame)
        val listFriendLayout: FrameLayout = findViewById(R.id.friend_layout)
        val friendLayout: FrameLayout = findViewById(R.id.friendFrame)
        val liveLayout: FrameLayout = findViewById(R.id.live_layout)
        val loginLayout: FrameLayout = findViewById(R.id.login_layout)
        switchFrame(friendLayout,drawerLayout,listLayout,homeLayout,splashLayout,listFriendLayout,carLayout,liveLayout,loginLayout)
        var extras = intent?.extras
        var sender = extras?.get("sender") as String
        var receiver = extras?.get("receiver") as String

        var buttonAccept:Button = findViewById(R.id.acceptFriendRequest)
        var friendTextView:TextView = findViewById(R.id.friendRequestText)
        friendTextView.text = sender + " ti ha inviato una richiesta di amicizia!"
        var buttonDecline:Button = findViewById(R.id.cancelFriendRequest)
        buttonAccept.setOnClickListener {
            confirmFriend(sender,receiver)
            switchFrame(homeLayout,drawerLayout,listLayout,friendLayout,listFriendLayout,splashLayout,carLayout,liveLayout,loginLayout)
            if(!isRunning) {
                val main = Intent(context,MapsActivity::class.java)
                zoom = 1
                startActivity(main)
            }
            finish()
        }
        buttonDecline.setOnClickListener {
            switchFrame(homeLayout,drawerLayout,listLayout,friendLayout,listFriendLayout,splashLayout,carLayout,liveLayout,loginLayout)
            if(!isRunning) {
                val main = Intent(context,MapsActivity::class.java)
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

}