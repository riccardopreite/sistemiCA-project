package com.example.maptry.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.ArrayAdapter
import com.example.maptry.R
import com.example.maptry.databinding.FragmentLiveEventsBinding
import com.example.maptry.domain.LiveEvents
import com.example.maptry.fragment.dialog.liveevents.LiveEventDetailsDialogFragment
import com.example.maptry.model.liveevents.LiveEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class LiveEventsFragment : Fragment(R.layout.fragment_live_events) {
    // UI
    private var _binding: FragmentLiveEventsBinding? = null
    private val binding get() = _binding!!

    // App state
    private lateinit var liveEventsList: MutableList<LiveEvent>

    companion object {
        private val TAG: String = LiveEventsFragment::class.qualifiedName!!

        private const val ARG_LIVEEVENTSLIST = "liveEventsList"

        @JvmStatic
        fun newInstance(liveEvents: List<LiveEvent>) =
            LiveEventsFragment().apply {
                arguments = Bundle().apply {
                    putParcelableArray(ARG_LIVEEVENTSLIST, liveEvents.toTypedArray())
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        arguments?.let {
            val pArray = it.getParcelableArray(ARG_LIVEEVENTSLIST)
            pArray?.let { p ->
                Log.d(TAG, "Loading liveEventsList from savedInstanceState")
                liveEventsList = MutableList(p.size) { i -> p[i] as LiveEvent }
            } ?: run {
                Log.e(TAG, "liveEventsList inside savedInstanceState was null. Loading an emptyList.")
                liveEventsList = emptyList<LiveEvent>().toMutableList()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.v(TAG, "onViewCreated")
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLiveEventsBinding.bind(view)

        binding.noLiveeventsItems.visibility = if(liveEventsList.isEmpty()) View.VISIBLE else View.INVISIBLE

        keepOnlyValidLiveEvents()

        binding.liveeventsListView.adapter = ArrayAdapter(view.context, android.R.layout.simple_list_item_1, liveEventsList.map { it.name })

        binding.liveeventsListView.setOnItemClickListener { parent, v, position, id ->
            val selectedLiveEventName = parent.getItemAtPosition(position) as String
            val liveEvent = liveEventsList.first { it.name == selectedLiveEventName }
            val markDialog = LiveEventDetailsDialogFragment.newInstance(liveEvent)
            activity?.let {
                markDialog.show(it.supportFragmentManager, "LiveEventDetailsDialogFragment")
            }
        }

        binding.closeLiveeventsFragment.setOnClickListener {
            activity?.finish()
        }

        binding.refreshLiveeventsListFragment.setOnClickListener{
            CoroutineScope(Dispatchers.IO).launch {
                liveEventsList.clear()
                liveEventsList.addAll(LiveEvents.getLiveEvents(true))
                CoroutineScope(Dispatchers.Main).launch {
                    binding.liveeventsListView.adapter = ArrayAdapter(view.context, android.R.layout.simple_list_item_1, liveEventsList.map { it.name })
                }
            }

        }
    }

    override fun onDestroyView() {
        Log.v(TAG, "onDestroyView")
        super.onDestroyView()
        _binding = null
    }

    private fun keepOnlyValidLiveEvents() {
        Log.v(TAG, "keepOnlyValidLiveEvents")
        val currentSeconds = Clock.System.now().epochSeconds // Seconds from Unix Epoch (UTC)
        val validLiveEvents = liveEventsList.filter { it.expirationDate > currentSeconds }
        liveEventsList.clear()
        liveEventsList.addAll(validLiveEvents)

        arguments?.putParcelableArray(ARG_LIVEEVENTSLIST, liveEventsList.toTypedArray())
    }
}