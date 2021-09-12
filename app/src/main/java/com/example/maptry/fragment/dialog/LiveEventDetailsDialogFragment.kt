package com.example.maptry.fragment.dialog

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
import com.example.maptry.model.pointofinterests.PointOfInterest
import org.w3c.dom.Text
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
            val dialogView = inflater.inflate(R.layout.dialog_custom_view, null)
            val titleTv = dialogView.findViewById<TextView>(R.id.headerattr)
            val addressTv = dialogView.findViewById<TextView>(R.id.txt_addressattr)
            val urlLbl: TextView = dialogView.findViewById(R.id.uri_lbl)
            val urlTv = dialogView.findViewById<TextView>(R.id.uri_lblattr)
            val phoneLbl = dialogView.findViewById<TextView>(R.id.phone_content)
            val phoneTv = dialogView.findViewById<TextView>(R.id.phone_contentattr)
            val addPoiToMyPoisBtn = dialogView.findViewById<Button>(R.id.addToPoisBtnattr)
            val routeBtn = dialogView.findViewById<Button>(R.id.routeBtn)

            titleTv.text = "Live: " + liveEvent.name
            addressTv.text = liveEvent.address
            phoneLbl.text = "Ending on"
            phoneTv.text = LocalDateTime.ofEpochSecond(liveEvent.expirationDate, 0, OffsetDateTime.now().offset).format(
                DateTimeFormatter.ISO_LOCAL_DATE_TIME
            )
            urlLbl.text = "Owner"
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
            throw ClassCastException("$context must implement FriendPoiDialogListener")
        }
    }
}