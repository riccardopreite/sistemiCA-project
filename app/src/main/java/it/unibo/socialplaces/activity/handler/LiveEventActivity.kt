package it.unibo.socialplaces.activity.handler

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import it.unibo.socialplaces.config.PushNotification
import it.unibo.socialplaces.model.liveevents.LiveEvent

class LiveEventActivity: AppCompatActivity() {
    companion object {
        private val TAG: String = LiveEventActivity::class.qualifiedName!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        val extras = intent.extras!!
        val notificationId = extras.getInt("notificationId")
        val live = extras.getParcelable<LiveEvent>("liveEvent")
        PushNotification.notificationManager.cancel(notificationId)

        Log.i(TAG, "A new live event has been published: $live.")
    }
}
