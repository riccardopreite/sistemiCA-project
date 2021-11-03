package it.unibo.socialplaces.activity.notification

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import it.unibo.socialplaces.config.PushNotification

class FriendAcceptedActivity: AppCompatActivity() {
    companion object {
        private val TAG: String = FriendAcceptedActivity::class.qualifiedName!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        val extras = intent.extras!!
        val friendUsername = extras.getString("friendUsername")
        val notificationId = extras.getInt("notificationId")
        println(friendUsername)
        PushNotification.notificationManager.cancel(notificationId)
        Log.v(TAG,"Here show friendUsername: $friendUsername on menu")
    }

}
