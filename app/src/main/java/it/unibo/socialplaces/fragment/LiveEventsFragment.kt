package it.unibo.socialplaces.fragment

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.ArrayAdapter
import it.unibo.socialplaces.R
import it.unibo.socialplaces.databinding.FragmentLiveEventsBinding
import it.unibo.socialplaces.domain.LiveEvents
import it.unibo.socialplaces.model.liveevents.LiveEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import java.lang.ClassCastException

class LiveEventsFragment : Fragment(R.layout.fragment_live_events) {
    // Listener
    interface LiveEventsListener {
        fun onLiveEventSelected(fragment: Fragment, leName: String)
    }

    internal lateinit var listener: LiveEventsListener

    // UI
    private var _binding: FragmentLiveEventsBinding? = null
    private val binding get() = _binding!!

    // App state
    private lateinit var liveEventsList: List<LiveEvent>

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
            liveEventsList = pArray?.let { p ->
                Log.d(TAG, "Loading liveEventsList from savedInstanceState")
                List(p.size) { i -> p[i] as LiveEvent }
            } ?: run {
                Log.e(TAG, "liveEventsList inside savedInstanceState was null. Loading an emptyList.")
                emptyList()
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

        binding.liveeventsListView.setOnItemClickListener { parent, _, position, _ ->
            val selectedLiveEventName = parent.getItemAtPosition(position) as String
            listener.onLiveEventSelected(this, selectedLiveEventName)
        }

        binding.closeLiveeventsFragment.setOnClickListener {
            activity?.finish()
        }

        binding.refreshLiveeventsListFragment.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                liveEventsList = LiveEvents.getLiveEvents(true)

                CoroutineScope(Dispatchers.Main).launch {
                    binding.liveeventsListView.adapter = ArrayAdapter(
                        view.context,
                        android.R.layout.simple_list_item_1,
                        liveEventsList.map { it.name }
                    )
                }
            }

        }
    }

    /**
     * Checks whether [liveEventsList] has only live events which are still available.
     * Updates the bundle [arguments].
     */
    private fun keepOnlyValidLiveEvents() {
        Log.v(TAG, "keepOnlyValidLiveEvents")
        val currentSeconds = Clock.System.now().epochSeconds // Seconds from Unix Epoch (UTC)
        liveEventsList = liveEventsList.filter { it.expirationDate > currentSeconds }

        arguments?.putParcelableArray(ARG_LIVEEVENTSLIST, liveEventsList.toTypedArray())
    }

    override fun onDestroyView() {
        Log.v(TAG, "onDestroyView")
        super.onDestroyView()
        _binding = null
    }

    override fun onAttach(context: Context) {
        Log.v(TAG, "onAttach")
        super.onAttach(context)

        try {
            listener = context as LiveEventsListener
        } catch(e: ClassCastException) {
            throw ClassCastException("$context must implement LiveEventsListener")
        }
    }
}