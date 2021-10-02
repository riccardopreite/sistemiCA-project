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
import com.example.maptry.model.pointofinterests.PointOfInterest
import java.lang.ClassCastException
import java.lang.IllegalStateException

class FriendPoiDialogFragment: DialogFragment() {
    // Listener
    interface FriendPoiDialogListener {
        fun onAddButtonPressed(dialog: DialogFragment, poi: PointOfInterest)
        fun onRouteButtonPressed(dialog: DialogFragment, address: String)
    }

    internal lateinit var listener: FriendPoiDialogListener

    // App state
    private lateinit var friendPoi: PointOfInterest

    companion object {
        private val TAG = FriendPoiDialogFragment::class.qualifiedName

        private const val ARG_FRIENDPOI = "friendPoi"

        @JvmStatic
        fun newInstance(friendPoi: PointOfInterest) =
            FriendDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_FRIENDPOI, friendPoi)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        arguments?.let {
            friendPoi = it.getParcelable(ARG_FRIENDPOI)!!
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Log.v(TAG, "onCreateDialog")
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            val dialogView = inflater.inflate(R.layout.dialog_friend_poi, null)
            val titleTv = dialogView.findViewById<TextView>(R.id.poi_name_header)
            val addressTv = dialogView.findViewById<TextView>(R.id.txt_addressattr)
            val urlTv = dialogView.findViewById<TextView>(R.id.uri_lblattr)
            val phoneTv = dialogView.findViewById<TextView>(R.id.phone_contentattr)
            val addPoiToMyPoisBtn = dialogView.findViewById<Button>(R.id.addToPoisBtnattr)
            val routeBtn = dialogView.findViewById<Button>(R.id.routeBtn)

            titleTv.text = getString(R.string.dialog_friend_poi_header, friendPoi.name, friendPoi.type)
            addressTv.text = friendPoi.address
            urlTv.text = friendPoi.url
            phoneTv.text = friendPoi.phoneNumber

            addPoiToMyPoisBtn.setOnClickListener {
                listener.onAddButtonPressed(this, friendPoi)
            }

            routeBtn.setOnClickListener {
                listener.onRouteButtonPressed(this, friendPoi.address)
            }

            builder.setView(dialogView)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onAttach(context: Context) {
        Log.v(TAG, "onAttach")
        super.onAttach(context)

        try {
            listener = context as FriendPoiDialogListener
        } catch(e: ClassCastException) {
            throw ClassCastException("$context must implement FriendPoiDialogListener")
        }
    }
}