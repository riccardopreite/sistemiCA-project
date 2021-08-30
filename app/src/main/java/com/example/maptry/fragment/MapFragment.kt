package com.example.maptry.fragment

import android.location.Location
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
import com.example.maptry.databinding.FragmentLiveEventsBinding
import com.example.maptry.databinding.FragmentMapBinding
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import java.io.Serializable

class MapFragment : Fragment(R.layout.fragment_map), OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener,
    GoogleMap.OnMyLocationClickListener {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

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
        googleMap.setOnMarkerClickListener(this)
        googleMap.setOnMapClickListener(this)
        // startLocationUpdates()
        // setUpMap()
        googleMap.setOnMyLocationClickListener(this)
    }

    override fun onMapClick(positionOnMap: LatLng) {
        TODO("Not yet implemented")
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        TODO("Not yet implemented")
    }

    override fun onMyLocationClick(location: Location) {
        TODO("Not yet implemented")
    }
}