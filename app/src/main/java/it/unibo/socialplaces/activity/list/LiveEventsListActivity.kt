package it.unibo.socialplaces.activity.list

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import it.unibo.socialplaces.R
import it.unibo.socialplaces.domain.LiveEvents
import it.unibo.socialplaces.domain.PointsOfInterest
import it.unibo.socialplaces.fragment.LiveEventsListFragment
import it.unibo.socialplaces.fragment.PointsOfInterestListFragment
import it.unibo.socialplaces.fragment.dialog.liveevents.LiveEventDetailsDialogFragment
import it.unibo.socialplaces.model.liveevents.LiveEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LiveEventsListActivity: it.unibo.socialplaces.activity.ListActivity(),
    LiveEventDetailsDialogFragment.LiveEventDetailsDialogListener,
    LiveEventsListFragment.LiveEventsListener {

    companion object {
        private val TAG: String = LiveEventsListActivity::class.qualifiedName!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Using findViewById(android.R.id.content) is a workaround for accessing a view instance
        val snackbar = Snackbar.make(findViewById(android.R.id.content), R.string.loading_liveevents, Snackbar.LENGTH_INDEFINITE)
        snackbar.show()

        updateLiveEventsList(snackbar)
    }

    /**
     * @see LiveEventsListFragment.LiveEventsListener.onLiveEventSelected
     */
    override fun onLiveEventSelected(fragment: Fragment, leName: String) {
        Log.v(TAG, "LiveEventsListFragment.LiveEventsListener.onLiveEventSelected")
        CoroutineScope(Dispatchers.IO).launch {
            val liveEvents = LiveEvents.getLiveEvents()
            val selectedLive = liveEvents.first { it.name == leName } // It surely exists.
            CoroutineScope(Dispatchers.Main).launch {
                val leDetailsDialogFragment = LiveEventDetailsDialogFragment.newInstance(selectedLive)
                leDetailsDialogFragment.show(supportFragmentManager, "LiveEventDetailsDialogFragment")
            }
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

    /**
     * Updates the live events list calling the SocialPlaces API and, if a loading snackbar
     * was pushed, dismisses the snackbar when the update is completed.
     */
    private fun updateLiveEventsList(snackbar: Snackbar? = null) {
        Log.v(TAG,"updateLiveEventsList")

        CoroutineScope(Dispatchers.IO).launch {
            val leList = LiveEvents.getLiveEvents(forceSync = true)
            val lesFragment = LiveEventsListFragment.newInstance(leList)
            pushFragment(lesFragment)
            CoroutineScope(Dispatchers.Main).launch { snackbar?.dismiss() }
        }
    }
}