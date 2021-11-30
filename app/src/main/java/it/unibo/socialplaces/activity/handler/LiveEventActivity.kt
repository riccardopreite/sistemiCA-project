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
            intent.getStringExtra(getString(R.string.extra_live_event_id))!!,
            intent.getStringExtra(getString(R.string.extra_live_event_address))!!,
            intent.getStringExtra(getString(R.string.extra_live_event_latitude))!!.toDouble(),
            intent.getStringExtra(getString(R.string.extra_live_event_longitude))!!.toDouble(),
            intent.getStringExtra(getString(R.string.extra_live_event_name))!!,
            intent.getStringExtra(getString(R.string.extra_live_event_owner))!!,
            intent.getStringExtra(getString(R.string.extra_live_event_expiration_date))!!.toLong()
        )

        Log.i(TAG, "A new live event has been published: $liveEvent.")
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            action = getString(R.string.activity_new_live_event)
            putExtra(getString(R.string.extra_live_event), liveEvent)
            putExtra(getString(R.string.extra_notification), true)
        }

        startActivity(notificationIntent)
        finish()
    }
}
