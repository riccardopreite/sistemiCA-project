package com.example.maptry.fragment.dialog.liveevents

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.maptry.R
import com.example.maptry.model.liveevents.LiveEvent
import java.lang.ClassCastException
import java.lang.IllegalStateException
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class LiveEventDetailsDialogFragment : DialogFragment() {
    // Listener
    interface LiveEventDetailsDialogListener {
        fun onShareButtonPressed(dialog: DialogFragment, liveEvent: LiveEvent)
        fun onRouteButtonPressed(dialog: DialogFragment, address: String)
    }

    internal lateinit var listener: LiveEventDetailsDialogListener

    // App state
    private lateinit var liveEvent: LiveEvent

    companion object {
        private val TAG = LiveEventDetailsDialogFragment::class.qualifiedName

        private const val ARG_LIVEEVENT = "liveEvent"

        @JvmStatic
        fun newInstance(liveEvent: LiveEvent) =
            LiveEventDetailsDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_LIVEEVENT, liveEvent)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        arguments?.let {
            liveEvent = it.getParcelable(ARG_LIVEEVENT)!!
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Log.v(TAG, "onCreateDialog")
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            val dialogView = inflater.inflate(R.layout.dialog_live_event_details, null)
            val titleTv = dialogView.findViewById<TextView>(R.id.live_event_name_header)
            val addressTv = dialogView.findViewById<TextView>(R.id.show_live_event_address)
            val urlTv = dialogView.findViewById<TextView>(R.id.show_owner)
            val phoneTv = dialogView.findViewById<TextView>(R.id.show_ending)
            val addPoiToMyPoisBtn = dialogView.findViewById<Button>(R.id.share_live_event)
            val routeBtn = dialogView.findViewById<Button>(R.id.navigate_to_poi)

            titleTv.text = getString(R.string.dialog_live_event_details, liveEvent.name)
            addressTv.text = liveEvent.address
            phoneTv.text = LocalDateTime.ofEpochSecond(liveEvent.expirationDate, 0, OffsetDateTime.now().offset).format(
                DateTimeFormatter.ISO_LOCAL_DATE_TIME
            )
            urlTv.text = liveEvent.owner

            addPoiToMyPoisBtn.setOnClickListener {
                listener.onShareButtonPressed(this, liveEvent)
            }

            routeBtn.setOnClickListener {
                listener.onRouteButtonPressed(this, liveEvent.address)
            }

            builder.setView(dialogView)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onAttach(context: Context) {
        Log.v(TAG, "onAttach")
        super.onAttach(context)

        try {
            listener = context as LiveEventDetailsDialogListener
        } catch(e: ClassCastException) {
            throw ClassCastException("$context must implement LiveEventDetailsDialogListener")
        }
    }
}