package it.unibo.socialplaces.fragment.dialog.pointsofinterest

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.DialogFragment
import it.unibo.socialplaces.R
import it.unibo.socialplaces.model.pointofinterests.PointOfInterest
import java.lang.ClassCastException
import java.lang.IllegalStateException
import android.content.DialogInterface
import android.widget.*
import androidx.core.view.isVisible
import it.unibo.socialplaces.domain.Recommendation
import it.unibo.socialplaces.model.recommendation.ValidationRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class PoiDetailsDialogFragment : DialogFragment() {
    companion object {
        private val TAG = PoiDetailsDialogFragment::class.qualifiedName!!

        private const val ARG_POI = "poi"
        private const val ARG_VALIDATIONREQUEST = "validationRequest"
        private const val ARG_RECOMMENDATIONRESULT = "recommendationResult"

        @JvmStatic
        fun newInstance(poi: PointOfInterest, validationRequest: ValidationRequest? = null) =
            PoiDetailsDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_POI, poi)
                    putParcelable(ARG_VALIDATIONREQUEST, validationRequest)
                    putString(ARG_RECOMMENDATIONRESULT, recommendationResult)
                }
            }
    }

    // Listener
    interface PoiDetailsDialogListener {
        fun onShareButtonPressed(dialog: DialogFragment, poi: PointOfInterest)
        fun onRouteButtonPressed(dialog: DialogFragment, address: String)
    }

    internal lateinit var listener: PoiDetailsDialogListener

    // App state
    private lateinit var poi: PointOfInterest
    private var validationRequest: ValidationRequest? = null
    private var recommendationResult: String = "yes"

    /**
     * Callback to be invoked after the dialog has been closed.
     */
    private lateinit var onDismissCallback: () -> Unit

    /**
     * Sets a callback to be invoked when the dialog gets dismissed.
     * @param onDismissCallback the callback to invoke.
     */
    fun setOnDismissCallback(onDismissCallback:() -> Unit) {
        this.onDismissCallback = onDismissCallback
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        arguments?.let {
            poi = it.getParcelable(ARG_POI)!!
            validationRequest = it.getParcelable(ARG_VALIDATIONREQUEST)
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
            val recommendationCheck1Layout = dialogView.findViewById<RelativeLayout>(R.id.recommendation_check_1_layout)
            val recommendationCheck2Layout = dialogView.findViewById<RelativeLayout>(R.id.recommendation_check_2_layout)

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

            validationRequest?.let {
                // rc stands for "recommendation check"
                val rcYesRadioBtn = dialogView.findViewById<RadioButton>(R.id.recommendation_check_yes)
                val rcRestaurantsRadioBtn = dialogView.findViewById<RadioButton>(R.id.recommendation_check_restaurants)
                val rcLeisureRadioBtn = dialogView.findViewById<RadioButton>(R.id.recommendation_check_leisure)
                val rcSportRadioBtn = dialogView.findViewById<RadioButton>(R.id.recommendation_check_sport)
                // Hiding the radio button of the category the point of interest is part of
                // It does not make sense to show it, otherwise the user could say that they wanted
                // a better point of interest, of the same category (which makes no sense).
                val radioBtnToHide = when(poi.type) {
                    getString(R.string.recommendation_check_restaurants) -> rcRestaurantsRadioBtn
                    getString(R.string.recommendation_check_leisure) -> rcLeisureRadioBtn
                    getString(R.string.recommendation_check_sport) -> rcSportRadioBtn
                    else -> null
                }
                if(radioBtnToHide != null) {
                    radioBtnToHide.isVisible = false
                }


                rcYesRadioBtn.setOnClickListener {
                    recommendationResult = "yes"
                }
                rcRestaurantsRadioBtn.setOnClickListener {
                    recommendationResult = getString(R.string.recommendation_check_restaurants)
                }
                rcLeisureRadioBtn.setOnClickListener {
                    recommendationResult = getString(R.string.recommendation_check_leisure)
                }
                rcSportRadioBtn.setOnClickListener {
                    recommendationResult = getString(R.string.recommendation_check_sport)
                }
            } ?: run {
                recommendationCheck1Layout.isVisible = false
                recommendationCheck2Layout.isVisible = false
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(ARG_POI, poi)
        outState.putParcelable(ARG_VALIDATIONREQUEST, validationRequest)
        outState.putString(ARG_RECOMMENDATIONRESULT, recommendationResult)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.let {
            poi = it.getParcelable(ARG_POI)!!
            validationRequest = it.getParcelable(ARG_VALIDATIONREQUEST)
            recommendationResult = it.getString(ARG_RECOMMENDATIONRESULT, "yes")
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        Log.v(TAG, "onDismiss")
        super.onDismiss(dialog)
        if(this::onDismissCallback.isInitialized){
            onDismissCallback()
        }
        validationRequest?.let { valReq ->
            CoroutineScope(Dispatchers.IO).launch {
                val trainModelData = if(recommendationResult == "yes") {
                    valReq
                } else {
                    valReq.copy(place_category = recommendationResult)
                }
                Recommendation.trainModel(trainModelData)
            }
        }
    }
}