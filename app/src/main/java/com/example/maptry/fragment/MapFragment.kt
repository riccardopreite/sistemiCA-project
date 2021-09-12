package com.example.maptry.fragment

import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import androidx.fragment.app.DialogFragment
import com.example.maptry.R
import com.example.maptry.databinding.FragmentMapBinding
import com.example.maptry.fragment.dialog.CreatePoiDialogFragment
import com.example.maptry.domain.LiveEvents
import com.example.maptry.domain.PointsOfInterest
import com.example.maptry.model.liveevents.AddLiveEvent
import com.example.maptry.model.pointofinterests.AddPointOfInterest
import com.example.maptry.model.pointofinterests.AddPointOfInterestPoi
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MapFragment : Fragment(R.layout.fragment_map), OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener,
    GoogleMap.OnMyLocationClickListener,
    CreatePoiDialogFragment.CreatePoiDialogListener {

    // UI
    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    // API
    private val geocoder by lazy {
        Geocoder(this.requireContext())
    }

    private val liveEvents by lazy {
        LiveEvents
    }

    private val pointsOfInterest by lazy {
        PointsOfInterest
    }

    companion object {
        val TAG = MapFragment::class.qualifiedName

        @JvmStatic
        fun newInstance() =
            MapFragment().apply {
                arguments = Bundle().apply {}
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        arguments?.let {}
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.v(TAG, "onViewCreated")
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentMapBinding.bind(view)
        val supportMapFragment = childFragmentManager.findFragmentById(binding.map.id) as SupportMapFragment

        supportMapFragment.getMapAsync(this)
        // TODO LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onDestroyView() {
        Log.v(TAG, "onDestroyView")
        super.onDestroyView()
        _binding = null
    }

    override fun onMapReady(googleMap: GoogleMap) {
        Log.v(TAG, "OnMapReadyCallback.onMapReady")
        googleMap.setOnMarkerClickListener(this)
        googleMap.setOnMapClickListener(this)
        // startLocationUpdates()
        // setUpMap()
        googleMap.setOnMyLocationClickListener(this)
    }

    override fun onMapClick(positionOnMap: LatLng) {
        Log.v(TAG, "GoogleMap.OnMapClickListener.onMapClick")
        TODO("Not yet implemented")
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        Log.v(TAG, "GoogleMap.OnMarkerClickListener.onMarkerClick")
        TODO("Not yet implemented")
    }

    override fun onMyLocationClick(location: Location) {
        Log.v(TAG, "GoogleMap.OnMyLocationClickListener.onMyLocationClick")
        TODO("Not yet implemented")
    }

    override fun onAddLiveEvent(dialog: DialogFragment, addLiveEvent: AddLiveEvent) {
        Log.v(TAG, "CreatePoiDialogListener.onAddLiveEvent")
        CoroutineScope(Dispatchers.IO).launch {
            liveEvents.addLiveEvent(addLiveEvent)

            CoroutineScope(Dispatchers.Main).launch {
                dialog.dismiss()
            }
        }
    }

    override fun onAddPointOfInterest(
        dialog: DialogFragment,
        addPointOfInterest: AddPointOfInterestPoi
    ) {
        Log.v(TAG, "CreatePoiDialogListener.onAddPointOfInterest")
        CoroutineScope(Dispatchers.IO).launch {
            pointsOfInterest.addPointOfInterest(AddPointOfInterest(addPointOfInterest))

            CoroutineScope(Dispatchers.Main).launch {
                dialog.dismiss()
            }
        }
    }
}