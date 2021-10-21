package it.unibo.socialplaces.fragment.dialog.pointsofinterest

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import it.unibo.socialplaces.R
import it.unibo.socialplaces.model.pointofinterests.PointOfInterest
import java.lang.ClassCastException
import java.lang.IllegalStateException

class PoiDetailsDialogFragment : DialogFragment() {
    // Listener
    interface PoiDetailsDialogListener {
        fun onShareButtonPressed(dialog: DialogFragment, poi: PointOfInterest)
        fun onRouteButtonPressed(dialog: DialogFragment, address: String)
    }

    internal lateinit var listener: PoiDetailsDialogListener

    // App state
    private lateinit var poi: PointOfInterest

    companion object {
        private val TAG = PoiDetailsDialogFragment::class.qualifiedName

        private const val ARG_POI = "poi"

        @JvmStatic
        fun newInstance(poi: PointOfInterest) =
            PoiDetailsDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_POI, poi)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        arguments?.let {
            poi = it.getParcelable(ARG_POI)!!
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Log.v(TAG, "onCreateDialog")
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            val dialogView = inflater.inflate(R.layout.dialog_poi_details, null)
            val titleTv = dialogView.findViewById<TextView>(R.id.poi_name_header)
            val addressTv = dialogView.findViewById<TextView>(R.id.show_poi_address)
            val urlTv = dialogView.findViewById<TextView>(R.id.show_website)
            val phoneTv = dialogView.findViewById<TextView>(R.id.show_phone_number)
            val shareBtn = dialogView.findViewById<Button>(R.id.share_poi)
            val routeBtn = dialogView.findViewById<Button>(R.id.navigate_to_poi)

            titleTv.text = getString(R.string.dialog_poi_details, poi.name, poi.type)
            addressTv.text = poi.address
            urlTv.text = poi.url
            phoneTv.text = poi.phoneNumber

            shareBtn.setOnClickListener {
                listener.onShareButtonPressed(this, poi)
            }

            routeBtn.setOnClickListener {
                listener.onRouteButtonPressed(this, poi.address)
            }

            builder.setView(dialogView)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onAttach(context: Context) {
        Log.v(TAG, "onAttach")
        super.onAttach(context)

        try {
            listener = context as PoiDetailsDialogListener
        } catch(e: ClassCastException) {
            throw ClassCastException("$context must implement FriendPoiDialogListener")
        }
    }
}