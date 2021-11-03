package it.unibo.socialplaces.activity.handler

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import it.unibo.socialplaces.config.PushNotification

class FriendRequestAcceptedActivity: AppCompatActivity() {
    companion object {
        private val TAG: String = FriendRequestAcceptedActivity::class.qualifiedName!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        val extras = intent.extras!!
        val friendUsername = extras.getString("friendUsername")
        val notificationId = extras.getInt("notificationId")
        PushNotification.notificationManager.cancel(notificationId)

        Log.i(TAG,"You are now friend with $friendUsername!")
    }

}