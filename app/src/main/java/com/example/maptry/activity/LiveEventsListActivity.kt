package com.example.maptry.activity

import android.os.Bundle
import com.example.maptry.domain.LiveEvents
import com.example.maptry.fragment.LiveEventsFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LiveEventsListActivity: ListActivity() {
    companion object {
        private val TAG: String = LiveEventsListActivity::class.qualifiedName!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CoroutineScope(Dispatchers.IO).launch {
            val liveEventsList = LiveEvents.getLiveEvents()
            val liveEventsFragment = LiveEventsFragment.newInstance(liveEventsList)
            pushFragment(liveEventsFragment)
        }
    }
}