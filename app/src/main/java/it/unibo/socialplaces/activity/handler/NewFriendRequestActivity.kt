package it.unibo.socialplaces.activity.handler

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import it.unibo.socialplaces.R
import it.unibo.socialplaces.activity.MainActivity

class NewFriendRequestActivity: AppCompatActivity(){
    companion object {
        private val TAG: String = NewFriendRequestActivity::class.qualifiedName!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "onCreate")
        super.onCreate(savedInstanceState)

        val friendUsername = intent.getStringExtra("friendUsername")

        Log.i(TAG,"Friend request from $friendUsername!")
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            action = getString(R.string.notification_new_friend_request)
            putExtra("friendUsername", friendUsername)
            putExtra("notification", true)
        }

        startActivity(notificationIntent)
        finish()
    }

}