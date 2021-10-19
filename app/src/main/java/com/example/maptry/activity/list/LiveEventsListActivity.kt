package com.example.maptry.activity.list

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.example.maptry.R
import com.example.maptry.activity.ListActivity
import com.example.maptry.domain.LiveEvents
import com.example.maptry.fragment.LiveEventsFragment
import com.example.maptry.fragment.dialog.liveevents.LiveEventDetailsDialogFragment
import com.example.maptry.model.liveevents.LiveEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LiveEventsListActivity: ListActivity(),
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

    override fun onRouteButtonPressed(dialog: DialogFragment, address: String) {
        Log.v(TAG, "LiveEventDetailsDialogFragment.onRouteButtonPressed")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$address"))
        dialog.dismiss()
        startActivity(intent)
    }

    override fun onShareButtonPressed(dialog: DialogFragment, liveEvent: LiveEvent) {
        Log.v(TAG, "LiveEventDetailsDialogFragment.onShareButtonPressed")
        sharePlace(liveEvent.name, liveEvent.address, liveEvent.latitude, liveEvent.longitude)
    }

    private fun sharePlace(name: String, address: String, latitude: Double, longitude: Double) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, getString(R.string.share_place, name, address, latitude, longitude))
        }
        val createdIntent = Intent.createChooser(shareIntent,getString(R.string.share_place_intent, name))
        ContextCompat.startActivity(this, createdIntent, null)
    }
}