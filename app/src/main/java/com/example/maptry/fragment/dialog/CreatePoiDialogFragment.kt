package com.example.maptry.fragment.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.example.maptry.R
import com.example.maptry.domain.LiveEvents
import com.example.maptry.domain.PointsOfInterest
import com.example.maptry.model.liveevents.AddLiveEvent
import com.example.maptry.model.pointofinterests.AddPointOfInterestPoi
import com.example.maptry.utils.makeRedLine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.ClassCastException
import java.lang.IllegalStateException

class CreatePoiDialogFragment: DialogFragment() {
    // Listener
    interface CreatePoiDialogListener {
        fun onAddLiveEvent(dialog: DialogFragment, addLiveEvent: AddLiveEvent)
        fun onAddPointOfInterest(dialog: DialogFragment, addPointOfInterest: AddPointOfInterestPoi)
    }

    internal lateinit var listener: CreatePoiDialogListener

    // App state
    private var latitude: Double ?= null
    private var longitude: Double ?= null
    private var address: String ?= null
    private var url: String ?= null
    private var phoneNumber: String ?= null

    // API
    private val liveEvents by lazy {
        LiveEvents
    }

    private val pointsOfInterest by lazy {
        PointsOfInterest
    }

    companion object {
        private val TAG = CreatePoiDialogFragment::class.qualifiedName

        private const val ARG_LATITUDE = "latitude"
        private const val ARG_LONGITUDE = "longitude"
        private const val ARG_ADDRESS = "address"
        private const val ARG_URL = "url"
        private const val ARG_PHONENUMBER = "phoneNumber"

        @JvmStatic
        fun newInstance(latitude: Double, longitude: Double, address: String?, url: String?, phoneNumber: String?) =
            CreatePoiDialogFragment().apply {
                arguments = Bundle().apply {
                    putDouble(ARG_LATITUDE, latitude)
                    putDouble(ARG_LONGITUDE, longitude)
                    putString(ARG_ADDRESS, address)
                    putString(ARG_URL, url)
                    putString(ARG_PHONENUMBER, phoneNumber)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        arguments?.let {
            latitude = it.getDouble(ARG_LATITUDE)
            longitude = it.getDouble(ARG_LONGITUDE)
            address = it.getString(ARG_ADDRESS)
            url = it.getString(ARG_URL)
            phoneNumber = it.getString(ARG_PHONENUMBER)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Log.v(TAG, "onCreateDialog")
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            val dialogView = inflater.inflate(R.layout.dialog_list_view, null)
            val typeSpinner = dialogView.findViewById<Spinner>(R.id.planets_spinner)
            val nameEt = dialogView.findViewById<EditText>(R.id.txt_lname)
            val addressTv = dialogView.findViewById<TextView>(R.id.txt_address)
            val visibilityRg = dialogView.findViewById<RelativeLayout>(R.id.rl_gender)
            val publicBtn = dialogView.findViewById<RadioButton>(R.id.rb_public)
            val privateBtn = dialogView.findViewById<RadioButton>(R.id.rb_private)
            val timePickerLayout = dialogView.findViewById<RelativeLayout>(R.id.timePicker)
            val durationTp = dialogView.findViewById<TimePicker>(R.id.timePicker1)
            // val addBtn = dialogView.findViewById<Button>(R.id.addBtn)
            // val cancelBtn = dialogView.findViewById<Button>(R.id.removeBtn)

            durationTp.setIs24HourView(true)
            durationTp.apply {
                hour = 3
                minute = 0
            }
            if(address != null) {
                addressTv.text = address
            }

            ArrayAdapter.createFromResource(it, R.array.planets_array, android.R.layout.simple_spinner_item).also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                typeSpinner.adapter = adapter
            }

            typeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val type = parent?.getItemAtPosition(position) as String
                    durationTp.setIs24HourView(true)
                    if(type == "Live event") {
                        visibilityRg.visibility = View.GONE
                        timePickerLayout.visibility = View.VISIBLE
                    } else {
                        timePickerLayout.visibility = View.GONE
                        visibilityRg.visibility = View.VISIBLE
                    }
                }
                override fun onNothingSelected(p0: AdapterView<*>?) {}
            }

            builder.setPositiveButton(R.string.add) { dialog, arg ->
                val red = R.color.quantum_googred
                val name = nameEt.text.toString()
                if(name == "") {
                    makeRedLine(nameEt, red)
                    return@setPositiveButton
                }

                val visibility = if(privateBtn.isChecked) {
                    privateBtn.text.toString()
                } else {
                    publicBtn.text.toString()
                }

                if(typeSpinner.selectedItem.toString() == "Live event") {
                    CoroutineScope(Dispatchers.IO).launch {
                        if (liveEvents.getLiveEvents().any { le ->
                            return@any if(name == le.name || addressTv.text == le.address) {
                                makeRedLine(nameEt, red)
                                true
                            } else { false }
                        }) { return@launch }
                        val time = durationTp.hour * 60 + durationTp.minute
                        // val marker = createMarker(p0)
                        listener.onAddLiveEvent(this@CreatePoiDialogFragment, AddLiveEvent(
                            time,
                            "",
                            name,
                            addressTv.text.toString(),
                            latitude!!,
                            longitude!!
                        ))
                    }
                    return@setPositiveButton
                }

                // Point of Interest
                CoroutineScope(Dispatchers.IO).launch {
                    if(pointsOfInterest.getPointsOfInterest().any { poi ->
                        return@any if(name == poi.name || addressTv.text == poi.address) {
                            makeRedLine(nameEt, red)
                            true
                        } else { false }
                    }) { return@launch }
                    // val marker = createMarker(p0)
                    listener.onAddPointOfInterest(this@CreatePoiDialogFragment, AddPointOfInterestPoi(
                        addressTv.text.toString(),
                        typeSpinner.selectedItem.toString(),
                        latitude!!,
                        longitude!!,
                        name,
                        if(phoneNumber != null) phoneNumber!! else "---",
                        visibility,
                        if(url != null) url!! else "---"
                    ))
                }
            }

            builder.setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }


            builder.setView(dialogView)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onAttach(context: Context) {
        Log.v(TAG, "onAttach")
        super.onAttach(context)

        try {
            listener = context as CreatePoiDialogListener
        } catch(e: ClassCastException) {
            throw ClassCastException("$context must implement CreatePoiDialogListener")
        }
    }
}