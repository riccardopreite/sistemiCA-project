package it.unibo.socialplaces.activity.handler

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import it.unibo.socialplaces.R
import it.unibo.socialplaces.activity.MainActivity

class FriendRequestAcceptedActivity : AppCompatActivity() {
    companion object {
        private val TAG: String = FriendRequestAcceptedActivity::class.qualifiedName!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "onCreate")
        super.onCreate(savedInstanceState)

        val friendUsername = intent.getStringExtra("friendUsername")

        Log.i(TAG, "You are now friends with $friendUsername!")
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            action = getString(R.string.activity_friend_request_accepted)
            putExtra("friendUsername", friendUsername)
            putExtra("notification", true)
        }

        startActivity(notificationIntent)
        finish()
    }

}