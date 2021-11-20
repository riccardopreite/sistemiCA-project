package it.unibo.socialplaces.fragment.dialog.pointsofinterest

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
import it.unibo.socialplaces.R
import it.unibo.socialplaces.domain.LiveEvents
import it.unibo.socialplaces.domain.PointsOfInterest
import it.unibo.socialplaces.model.liveevents.AddLiveEvent
import it.unibo.socialplaces.model.pointofinterests.AddPointOfInterestPoi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.ClassCastException
import java.lang.IllegalStateException
import android.widget.Toast

class CreatePoiOrLiveDialogFragment: DialogFragment() {
    // Listener
    interface CreatePoiOrLiveDialogListener {
        fun onAddLiveEvent(dialog: DialogFragment, addLiveEvent: AddLiveEvent)
        fun onAddPointOfInterest(dialog: DialogFragment, addPointOfInterestPoi: AddPointOfInterestPoi)
    }

    internal lateinit var listener: CreatePoiOrLiveDialogListener

    // App state
    private var latitude: Double ?= null
    private var longitude: Double ?= null
    private var address: String ?= null
    private var url: String ?= null
    private var phoneNumber: String ?= null

    companion object {
        private val TAG = CreatePoiOrLiveDialogFragment::class.qualifiedName!!

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

            builder.setView(dialogView)
            builder.setPositiveButton(R.string.create_poi_or_live,null)
            builder.setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }

            val dialog = builder.create()
            dialog.setOnShowListener {
                val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                positiveButton.setOnClickListener {
                    val name = nameEt.text.toString()
                    Log.v(TAG, "Inserted name $name")
                    if(name == "") {
                        displayInsertedDataError(nameEt, "Please insert a name.")
                    } else {
                        val visibility = if(privateBtn.isChecked) privateBtn.text.toString() else publicBtn.text.toString()

                        if(typeSpinner.selectedItem.toString() == "Live event") {
                            CoroutineScope(Dispatchers.IO).launch {
                                if(!LiveEvents.getLiveEvents().any { le ->
                                    return@any when {
                                        name == le.name -> {
                                            displayInsertedDataError(nameEt, "A Live event with the same name already exists.")
                                            true
                                        }
                                        addressTv.text == le.address -> {
                                            displayInsertedDataError(nameEt, "A Live event with the same address already exists.")
                                            true
                                        }
                                        else -> false
                                    }
                                }) {
                                    val timeInSeconds = durationTp.hour * 3600 + durationTp.minute * 60
                                    listener.onAddLiveEvent(this@CreatePoiOrLiveDialogFragment, AddLiveEvent(
                                        timeInSeconds,
                                        "",
                                        name,
                                        addressTv.text.toString(),
                                        latitude!!,
                                        longitude!!
                                    ))
                                }
                            }
                        } else { // Point of Interest
                            CoroutineScope(Dispatchers.IO).launch {
                                if(!PointsOfInterest.getPointsOfInterest().any { poi ->
                                        return@any when {
                                            name == poi.name -> {
                                                displayInsertedDataError(nameEt, "A Poi with the same name already exists.")
                                                true
                                            }
                                            addressTv.text == poi.address -> {
                                                displayInsertedDataError(nameEt, "A Poi with the same address already exists.")
                                                true
                                            }
                                            else -> false
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
                                }
                            }
                        }
                    }
                }
            }
            return dialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    /**
     * Displays a red line under one of the EditTexts of the poi/live event when an error
     * occurs.
     * @param editText the field that gets the error displayed.
     * @param displayedMessage the messaget to display.
     */
    private fun displayInsertedDataError(editText: EditText, displayedMessage: String) {
        CoroutineScope(Dispatchers.Main).launch {
            editText.background.mutate().apply {
                colorFilter = BlendModeColorFilter(
                    ContextCompat.getColor(requireContext(), R.color.quantum_googred),
                    BlendMode.SRC_IN
                )
            }
            Toast.makeText(context, displayedMessage, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onAttach(context: Context) {
        Log.v(TAG, "onAttach")
        super.onAttach(context)

        try {
            listener = context as CreatePoiOrLiveDialogListener
        } catch(e: ClassCastException) {
            throw ClassCastException("$context must implement CreatePoiDialogListener")
        }
    }
}