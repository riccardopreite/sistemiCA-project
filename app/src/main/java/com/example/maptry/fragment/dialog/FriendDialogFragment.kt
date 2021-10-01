package com.example.maptry.fragment.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.example.maptry.R
import com.example.maptry.domain.PointsOfInterest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.IllegalStateException

class FriendDialogFragment: DialogFragment() {
    // App state
    private lateinit var friendName: String

    companion object {
        private val TAG: String = FriendDialogFragment::class.qualifiedName!!

        private const val ARG_FRIENDNAME = "friendName"

        @JvmStatic
        fun newInstance(friendName: String) =
            FriendDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_FRIENDNAME, friendName)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        arguments?.let {
            friendName = it.getString(ARG_FRIENDNAME, "")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Log.v(TAG, "onCreateDialog")
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            val dialogView = inflater.inflate(R.layout.dialog_friend_view, null)
            val friendNameTv = dialogView.findViewById<TextView>(R.id.friendNameTxt)
            val poisSpinner = dialogView.findViewById<Spinner>(R.id.planets_spinner_POI)
            friendNameTv.text = friendName

            CoroutineScope(Dispatchers.IO).launch {
                val friendPois = PointsOfInterest.getPointsOfInterest(friendName)
                val poiNamesList = MutableList(1) { "" }
                poiNamesList.addAll(friendPois.map { p -> p.name })

                poisSpinner.adapter = ArrayAdapter(it, R.layout.support_simple_spinner_dropdown_item, poiNamesList)
                poisSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        val selectedPoiName = parent?.getItemAtPosition(position) as String
                        if(selectedPoiName == "") {
                            return
                        }

                        val friendPoi = friendPois.first { p -> p.name == selectedPoiName }

                        val friendPoiDialog = FriendPoiDialogFragment.newInstance(friendPoi)

                        friendPoiDialog.show(it.supportFragmentManager, "FriendPoiDialogFragment")
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        Log.v(TAG, "poisSpinner.onNothingSelected triggered.")
                    }
                }
            }

            builder.setView(dialogView)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}