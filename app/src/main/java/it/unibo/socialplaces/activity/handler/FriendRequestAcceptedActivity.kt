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
        val notificationId = intent.getIntExtra("notificationId", -1)
        val friendUsername = intent.getStringExtra("friendUsername")
        PushNotification.cancelNotification(notificationId)

        Log.i(TAG,"You are now friend with $friendUsername!")
    }

}