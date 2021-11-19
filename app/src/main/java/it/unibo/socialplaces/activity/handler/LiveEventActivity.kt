package it.unibo.socialplaces.activity.handler

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import it.unibo.socialplaces.R
import it.unibo.socialplaces.activity.MainActivity
import it.unibo.socialplaces.model.liveevents.LiveEvent

class LiveEventActivity: AppCompatActivity() {
    companion object {
        private val TAG: String = LiveEventActivity::class.qualifiedName!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "onCreate")
        super.onCreate(savedInstanceState)

        val liveEvent = LiveEvent(
            intent.getStringExtra("id")!!,
            intent.getStringExtra("address")!!,
            intent.getStringExtra("latitude")!!.toDouble(),
            intent.getStringExtra("longitude")!!.toDouble(),
            intent.getStringExtra("name")!!,
            intent.getStringExtra("owner")!!,
            intent.getStringExtra("expirationDate")!!.toLong()
        )

        Log.i(TAG, "A new live event has been published: $liveEvent.")
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            action = getString(R.string.activity_new_live_event)
            putExtra("liveEvent", liveEvent)
            putExtra("notification", true)
        }

        startActivity(notificationIntent)
        finish()
    }
}
