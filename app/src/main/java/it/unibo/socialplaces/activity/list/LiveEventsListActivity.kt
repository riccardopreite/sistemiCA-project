package it.unibo.socialplaces.activity.list

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import it.unibo.socialplaces.R
import it.unibo.socialplaces.activity.ListActivity
import it.unibo.socialplaces.domain.LiveEvents
import it.unibo.socialplaces.fragment.LiveEventsFragment
import it.unibo.socialplaces.fragment.dialog.liveevents.LiveEventDetailsDialogFragment
import it.unibo.socialplaces.model.liveevents.LiveEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LiveEventsListActivity: it.unibo.socialplaces.activity.ListActivity(),
    LiveEventDetailsDialogFragment.LiveEventDetailsDialogListener {

    companion object {
        private val TAG: String = LiveEventsListActivity::class.qualifiedName!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CoroutineScope(Dispatchers.IO).launch {
            val liveEventsList = LiveEvents.getLiveEvents(true)
            val liveEventsFragment = LiveEventsFragment.newInstance(liveEventsList)
            pushFragment(liveEventsFragment)
        }
    }

    /**
     * @see LiveEventDetailsDialogFragment.LiveEventDetailsDialogListener.onRouteButtonPressed
     */
    override fun onRouteButtonPressed(dialog: DialogFragment, address: String) {
        Log.v(TAG, "LiveEventDetailsDialogFragment.onRouteButtonPressed")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$address"))
        dialog.dismiss()
        startActivity(intent)
    }

    /**
     * @see LiveEventDetailsDialogFragment.LiveEventDetailsDialogListener.onShareButtonPressed
     */
    override fun onShareButtonPressed(dialog: DialogFragment, liveEvent: LiveEvent) {
        Log.v(TAG, "LiveEventDetailsDialogFragment.onShareButtonPressed")
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            with(liveEvent) {
                putExtra(
                    Intent.EXTRA_TEXT,
                    getString(R.string.share_place, name, address, latitude, longitude)
                )
            }

        }
        val createdIntent = Intent.createChooser(
            shareIntent,
            getString(R.string.share_place_intent, liveEvent.name)
        )
        ContextCompat.startActivity(this, createdIntent, null)
    }
}