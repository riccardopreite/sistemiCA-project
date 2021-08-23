package com.example.maptry.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.example.maptry.R
import com.example.maptry.config.CreatePointOfInterestOrLiveEvent
import com.example.maptry.config.GetPointOfInterestDetail
import com.example.maptry.config.GoogleMapsManager
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import java.io.Serializable

/**
 * A simple [Fragment] subclass.
 * Use the [MapFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MapFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var getPointOfInterestDetail: GetPointOfInterestDetail<AppCompatActivity>? = null
    private var createPointOfInterestOrLiveEvent: CreatePointOfInterestOrLiveEvent<AppCompatActivity>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        arguments?.let {
            getPointOfInterestDetail = it.getSerializable("getPointOfInterestDetail") as GetPointOfInterestDetail<AppCompatActivity>
            createPointOfInterestOrLiveEvent = it.getSerializable("createPointOfInterestOrLiveEvent") as CreatePointOfInterestOrLiveEvent<AppCompatActivity>
        }
    }

    companion object {
        val TAG = MapFragment::class.qualifiedName
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param getPointOfInterestDetail function reference that should display the details of a point of interest (if exists) in the position given as argument to the function
         * @param createPointOfInterestOrLiveEvent function reference that should allow creating a new point of interest/live event.
         * @return A new instance of fragment MapFragment.
         */
        @JvmStatic
        fun newInstance(getPointOfInterestDetail: GetPointOfInterestDetail<AppCompatActivity>, createPointOfInterestOrLiveEvent: CreatePointOfInterestOrLiveEvent<AppCompatActivity>) =
            MapFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(
                        "getPointOfInterestDetail",
                        getPointOfInterestDetail
                    )
                    putSerializable(
                        "createPointOfInterestOrLiveEvent",
                        createPointOfInterestOrLiveEvent
                    )
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.v(TAG, "onCreateView")
        val view = inflater.inflate(R.layout.fragment_map, container, false)

        val supportMapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment

//        supportMapFragment.getMapAsync(
//            GoogleMapsManager(
//                getPointOfInterestDetail.getPointOfInterestDetail,
//                createPointOfInterestOrLiveEvent.createPointOfInterestOrLiveEvent
//            )
//        )

        return view
    }
}