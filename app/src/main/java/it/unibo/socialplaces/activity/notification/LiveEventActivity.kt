package it.unibo.socialplaces.activity.notification

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
        val live = extras.get("liveEvent") as LiveEvent
        val notificationId = extras.getInt("notificationId")
        println(live)
        PushNotification.getManager().cancel(notificationId)
        Log.v(TAG,"Here show live on map")
    }
}
