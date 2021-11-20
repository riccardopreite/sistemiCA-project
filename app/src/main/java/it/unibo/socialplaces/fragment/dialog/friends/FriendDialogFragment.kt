package it.unibo.socialplaces.fragment.dialog.friends

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.fragment.app.DialogFragment
import it.unibo.socialplaces.R
import it.unibo.socialplaces.domain.PointsOfInterest
import it.unibo.socialplaces.model.pointofinterests.PointOfInterest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.ClassCastException
import java.lang.IllegalStateException

class FriendDialogFragment: DialogFragment() {
    // Listener
    interface FriendDialogListener {
        fun onPointOfInterestSelected(dialog: DialogFragment, friendPoi: PointOfInterest)
        fun removeFriend(dialog: DialogFragment, friendUsername: String)
    }

    internal lateinit var listener: FriendDialogListener

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
        return activity?.let { activity ->
            val builder = AlertDialog.Builder(activity)
            val inflater = requireActivity().layoutInflater
            val dialogView = inflater.inflate(R.layout.dialog_friend, null)
            val friendNameTv = dialogView.findViewById<TextView>(R.id.show_friend_name)
            val poisSpinner = dialogView.findViewById<Spinner>(R.id.select_friend_poi)
            val removeFriendBtn = dialogView.findViewById<Button>(R.id.remove_friend)
            friendNameTv.text = friendName

            removeFriendBtn.setOnClickListener {
                listener.removeFriend(this, friendName)
            }

            CoroutineScope(Dispatchers.IO).launch {
                val friendPois = PointsOfInterest.getPointsOfInterest(friendName)
                val poiNamesList = MutableList(1) { getString(R.string.no_poi_selected) }
                poiNamesList.addAll(friendPois.map { p -> p.name })

                CoroutineScope(Dispatchers.Main).launch {
                    poisSpinner.adapter = ArrayAdapter(
                        activity,
                        R.layout.support_simple_spinner_dropdown_item,
                        poiNamesList
                    )
                    poisSpinner.onItemSelectedListener =
                        object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(
                                parent: AdapterView<*>?,
                                view: View?,
                                position: Int,
                                id: Long
                            ) {
                                val selectedPoiName = parent?.getItemAtPosition(position) as String
                                Log.d(TAG, "Selected: $selectedPoiName")
                                if (selectedPoiName == getString(R.string.no_poi_selected)) {
                                    Log.w(TAG, "Selected a placeholder point. Not displaying it!")
                                    return
                                }

                                val friendPoi = friendPois.first { p -> p.name == selectedPoiName }
                                Log.d(TAG, friendPoi.toString())

                                Log.d(TAG, "$listener")
                                listener.onPointOfInterestSelected(this@FriendDialogFragment, friendPoi)
                            }

                            override fun onNothingSelected(parent: AdapterView<*>?) {
                                Log.v(TAG, "poisSpinner.onNothingSelected triggered.")
                            }
                        }
                }
            }

            builder.setView(dialogView)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onAttach(context: Context) {
        Log.v(TAG, "onAttach")
        super.onAttach(context)

        try {
            Log.d(TAG,"Context: $context")
            listener = context as FriendDialogListener
        } catch(e: ClassCastException) {
            throw ClassCastException("$context must implement FriendDialogListener")
        }
    }
}