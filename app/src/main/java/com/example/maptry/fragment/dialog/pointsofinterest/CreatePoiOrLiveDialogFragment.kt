package com.example.maptry.fragment.dialog.pointsofinterest

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.example.maptry.R
import com.example.maptry.domain.LiveEvents
import com.example.maptry.domain.PointsOfInterest
import com.example.maptry.model.liveevents.AddLiveEvent
import com.example.maptry.model.pointofinterests.AddPointOfInterestPoi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.ClassCastException
import java.lang.IllegalStateException
import android.widget.Toast




class CreatePoiOrLiveDialogFragment: DialogFragment() {
    // Listener
    interface CreatePoiDialogListener {
        fun onAddLiveEvent(dialog: DialogFragment, addLiveEvent: AddLiveEvent)
        fun onAddPointOfInterest(dialog: DialogFragment, addPointOfInterestPoi: AddPointOfInterestPoi)
    }

    internal lateinit var listener: CreatePoiDialogListener

    // App state
    private var latitude: Double ?= null
    private var longitude: Double ?= null
    private var address: String ?= null
    private var url: String ?= null
    private var phoneNumber: String ?= null

    companion object {
        private val TAG = CreatePoiOrLiveDialogFragment::class.qualifiedName

        private const val ARG_LATITUDE = "latitude"
        private const val ARG_LONGITUDE = "longitude"
        private const val ARG_ADDRESS = "address"
        private const val ARG_URL = "url"
        private const val ARG_PHONENUMBER = "phoneNumber"

        @JvmStatic
        fun newInstance(latitude: Double, longitude: Double, address: String? = null, url: String? = null, phoneNumber: String? = null) =
            CreatePoiOrLiveDialogFragment().apply {
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
            val dialogView = inflater.inflate(R.layout.dialog_create_poi_or_live, null)
            val typeSpinner = dialogView.findViewById<Spinner>(R.id.edit_place_type)
            val nameEt = dialogView.findViewById<EditText>(R.id.edit_place_name)
            val addressTv = dialogView.findViewById<TextView>(R.id.edit_address)
            val visibilityRg = dialogView.findViewById<RelativeLayout>(R.id.visibility_relative_layout)
            val publicBtn = dialogView.findViewById<RadioButton>(R.id.edit_visibility_public)
            val privateBtn = dialogView.findViewById<RadioButton>(R.id.edit_visibility_private)
            val timePickerLayout = dialogView.findViewById<RelativeLayout>(R.id.live_duration_relative_layout)
            val liveDurationLbl = dialogView.findViewById<TextView>(R.id.live_duration)
            val durationTp = dialogView.findViewById<TimePicker>(R.id.edit_live_duration)

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
                        durationTp.setIs24HourView(true)
                        durationTp.apply {
                            hour = 3
                            minute = 0
                        }
                        visibilityRg.visibility = View.GONE
                        liveDurationLbl.visibility = View.VISIBLE
                        timePickerLayout.visibility = View.VISIBLE
                    } else {
                        timePickerLayout.visibility = View.GONE
                        visibilityRg.visibility = View.VISIBLE
                    }
                }
                override fun onNothingSelected(p0: AdapterView<*>?) {}
            }

            //builder.setPositiveButton(R.string.create_poi_or_live) { dialog, arg ->            }
            builder.setPositiveButton(R.string.create_poi_or_live,null)
            builder.setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }


            builder.setView(dialogView)
            val dialog = builder.create()
            dialog.setOnShowListener {
                val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                positiveButton.setOnClickListener {
                    val name = nameEt.text.toString()
                    Log.v(TAG, "Inserted name $name")
                    if(name == "") {
                        nameEt.background.mutate().apply {
                            colorFilter = BlendModeColorFilter(
                                ContextCompat.getColor(requireContext(), R.color.quantum_googred),
                                BlendMode.SRC_IN
                            )
                        }
                        Toast.makeText(context, "Please insert a name.", Toast.LENGTH_SHORT).show()
                    }
                    else{
                        val visibility = if(privateBtn.isChecked) {
                            privateBtn.text.toString()
                        } else {
                            publicBtn.text.toString()
                        }

                        if(typeSpinner.selectedItem.toString() == "Live event") {
                            CoroutineScope(Dispatchers.IO).launch {
                                if(!LiveEvents.getLiveEvents().any { le ->
                                        Log.v(TAG,"LIVE NAME "+le.name)
                                        return@any when {
                                            name == le.name -> {
                                                requireActivity().runOnUiThread {
                                                    Toast.makeText(context, "A Live event with same name already exist.", Toast.LENGTH_SHORT).show()
                                                }
                                                nameEt.background.mutate().apply {
                                                    colorFilter = BlendModeColorFilter(
                                                        ContextCompat.getColor(requireContext(), R.color.quantum_googred),
                                                        BlendMode.SRC_IN
                                                    )
                                                }
                                                true
                                            }
                                            addressTv.text == le.address -> {
                                                requireActivity().runOnUiThread {
                                                    Toast.makeText(context, "A Live event with same address already exist.", Toast.LENGTH_SHORT).show()
                                                }
                                                nameEt.background.mutate().apply {
                                                    colorFilter = BlendModeColorFilter(
                                                        ContextCompat.getColor(requireContext(), R.color.quantum_googred),
                                                        BlendMode.SRC_IN
                                                    )
                                                }
                                                true
                                            }
                                            else -> { false }
                                        }

                                    }){
                                    val time = durationTp.hour * 60 + durationTp.minute
                                    listener.onAddLiveEvent(this@CreatePoiOrLiveDialogFragment, AddLiveEvent(
                                        time,
                                        "",
                                        name,
                                        addressTv.text.toString(),
                                        latitude!!,
                                        longitude!!
                                    ))
                                }
                            }
                        }
                        else{
                            // Point of Interest
                            CoroutineScope(Dispatchers.IO).launch {
                                if(!PointsOfInterest.getPointsOfInterest().any { poi ->

                                        return@any when {
                                            name == poi.name -> {
                                                requireActivity().runOnUiThread {
                                                    Toast.makeText(context, "A Poi with same name already exist.", Toast.LENGTH_SHORT).show()
                                                }
                                                nameEt.background.mutate().apply {
                                                    colorFilter = BlendModeColorFilter(
                                                        ContextCompat.getColor(requireContext(), R.color.quantum_googred),
                                                        BlendMode.SRC_IN
                                                    )
                                                }
                                                true
                                            }
                                            addressTv.text == poi.address -> {
                                                requireActivity().runOnUiThread {
                                                    Toast.makeText(context, "A Poi with same address already exist.", Toast.LENGTH_SHORT).show()
                                                }
                                                nameEt.background.mutate().apply {
                                                    colorFilter = BlendModeColorFilter(
                                                        ContextCompat.getColor(requireContext(), R.color.quantum_googred),
                                                        BlendMode.SRC_IN
                                                    )
                                                }
                                                true
                                            }
                                            else -> { false }
                                        }
                                    }) {
                                    listener.onAddPointOfInterest(this@CreatePoiOrLiveDialogFragment, AddPointOfInterestPoi(
                                        addressTv.text.toString(),
                                        typeSpinner.selectedItem.toString(),
                                        latitude!!,
                                        longitude!!,
                                        name,
                                        if(phoneNumber != null) phoneNumber!! else "---",
                                        visibility,
                                        if(url != null) url!! else "---"
                                    ))
                                       // return@launch
                                }
                                // val marker = createMarker(p0)

                            }
                        }


                    }
                }
            }

            return dialog

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