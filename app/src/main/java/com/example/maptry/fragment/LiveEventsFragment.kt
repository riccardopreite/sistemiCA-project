package com.example.maptry.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.ArrayAdapter
import com.example.maptry.R
import com.example.maptry.databinding.FragmentLiveEventsBinding
import com.example.maptry.model.liveevents.LiveEvent
import com.google.android.gms.maps.model.LatLng
import java.time.LocalDateTime
import java.time.ZoneOffset

class LiveEventsFragment : Fragment(R.layout.fragment_live_events) {
    private var _binding: FragmentLiveEventsBinding? = null
    private val binding get() = _binding!!

    private lateinit var liveEventsList: MutableList<LiveEvent>

    companion object {
        private val TAG: String = LiveEventsFragment::class.qualifiedName!!

        private const val ARG_LIVEEVENTSLIST = "liveEventsList"

        @JvmStatic
        fun newInstance(liveEvents: MutableList<LiveEvent>) =
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

        binding.nolive.visibility = if(liveEventsList.isEmpty()) View.VISIBLE else View.INVISIBLE

        keepOnlyValidLiveEvents()

        binding.lvLive.adapter = ArrayAdapter(view.context, android.R.layout.simple_list_item_1, liveEventsList.map { it.name })

        binding.lvLive.setOnItemClickListener { parent, v, position, id ->
            val selectedLiveEventName = parent.getItemAtPosition(position) as String
            val liveEvent = liveEventsList.first { it.name == selectedLiveEventName }
            val markerId = LatLng(liveEvent.latitude, liveEvent.longitude)
            // TODO Sarebbe da invocare MapsActivity.onMarkerClick(mymarker[markerId]!!)
        }
    }

    override fun onDestroyView() {
        Log.v(TAG, "onDestroyView")
        super.onDestroyView()
        _binding = null
    }

    private fun keepOnlyValidLiveEvents() {
        val validLiveEvents = liveEventsList.filter { it.expirationDate > LocalDateTime.now().atZone(
            ZoneOffset.systemDefault()).toInstant().toEpochMilli() }
        liveEventsList.clear()
        liveEventsList.addAll(validLiveEvents)

        arguments?.putParcelableArray(ARG_LIVEEVENTSLIST, liveEventsList.toTypedArray())
    }
}