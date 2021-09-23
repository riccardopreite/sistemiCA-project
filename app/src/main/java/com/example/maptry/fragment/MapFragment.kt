package com.example.maptry.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.example.maptry.R
import com.example.maptry.ui.CircleTransform
import com.example.maptry.config.Auth
import com.example.maptry.databinding.FragmentMapBinding
import com.example.maptry.fragment.dialog.CreatePoiDialogFragment
import com.example.maptry.domain.LiveEvents
import com.example.maptry.domain.PointsOfInterest
import com.example.maptry.fragment.dialog.LiveEventDetailsDialogFragment
import com.example.maptry.fragment.dialog.PoiDetailsDialogFragment
import com.example.maptry.model.liveevents.AddLiveEvent
import com.example.maptry.model.liveevents.LiveEvent
import com.example.maptry.model.pointofinterests.AddPointOfInterest
import com.example.maptry.model.pointofinterests.AddPointOfInterestPoi
import com.example.maptry.model.pointofinterests.PointOfInterest
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException

class MapFragment : Fragment(R.layout.fragment_map),
    OnMapReadyCallback,
    GoogleMap.OnMapClickListener,
    GoogleMap.OnMarkerClickListener,
    GoogleMap.OnMyLocationClickListener {

    // UI
    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    // App state
    private lateinit var poisList: List<PointOfInterest>
    private lateinit var liveEventsList: List<LiveEvent>
    private val markers: MutableMap<String, Marker> = emptyMap<String, Marker>().toMutableMap()

    // API
    private val geocoder by lazy {
        Geocoder(this.requireContext())
    }

    companion object {
        val TAG = MapFragment::class.qualifiedName

        private const val ARG_POISLIST = "poisList"
        private const val ARG_LIVEEVENTSLIST = "liveEventsList"

        @JvmStatic
        fun newInstance(poisList: List<PointOfInterest>, liveEventsList: List<LiveEvent>) =
            MapFragment().apply {
                arguments = Bundle().apply {
                    putParcelableArray(ARG_POISLIST, poisList.toTypedArray())
                    putParcelableArray(ARG_LIVEEVENTSLIST, liveEventsList.toTypedArray())
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        arguments?.let {
            val pArrayPois = it.getParcelableArray(ARG_POISLIST)
            pArrayPois?.let { p ->
                Log.d(TAG, "Loading poisList from savedInstanceState")
                poisList = MutableList(p.size) { i -> p[i] as PointOfInterest }
            } ?: run {
                Log.e(TAG, "poisList inside savedInstanceState was null. Loading an emptyList.")
                poisList = emptyList<PointOfInterest>().toMutableList()
            }

            val pArrayLive = it.getParcelableArray(ARG_LIVEEVENTSLIST)
            pArrayLive?.let { p ->
                Log.d(TAG, "Loading liveEventsList from savedInstanceState")
                liveEventsList = MutableList(p.size) { i -> p[i] as LiveEvent }
            } ?: run {
                Log.e(TAG, "liveEventsList inside savedInstanceState was null. Loading an emptyList.")
                liveEventsList = emptyList<LiveEvent>().toMutableList()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.v(TAG, "onViewCreated")
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentMapBinding.bind(view)

        val supportMapFragment = childFragmentManager.findFragmentById(binding.map.id) as SupportMapFragment
        updateMapUI(supportMapFragment)
        supportMapFragment.getMapAsync(this)

        activity?.let { a ->
            val autoCompleteFragment = childFragmentManager.findFragmentById(R.id.autocomplete_fragment) as AutocompleteSupportFragment
            autoCompleteFragment.view?.let {
                val layout = it as LinearLayout
                Log.d(TAG, "autoCompleteFragment.view exists.")
                val menuIcon = layout.getChildAt(0) as ImageView

                Picasso.get()
                    .load(Auth.getUserProfileIcon())
                    .transform(CircleTransform())
                    .resize(100, 100)
                    .into(menuIcon)

                menuIcon.setOnClickListener {
                    val mainMenuFragment = MainMenuFragment()
                    a.supportFragmentManager.beginTransaction().apply {
                        replace(R.id.main_menu_fragment, mainMenuFragment)
                        setReorderingAllowed(true)
//                        addToBackStack("MainMenuFragment")
                        commit()
                    }
                }
            } ?: run {
                Log.e(TAG, "autoCompleteFragment.view is null!")
            }
        }
    }

    override fun onDestroyView() {
        Log.v(TAG, "onDestroyView")
        super.onDestroyView()
        _binding = null
    }

    @SuppressLint("PotentialBehaviorOverride") // per setOnMarkerClickListener
    override fun onMapReady(googleMap: GoogleMap) {
        Log.v(TAG, "OnMapReadyCallback.onMapReady")
        googleMap.setOnMarkerClickListener(this)
        googleMap.setOnMapClickListener(this)
        googleMap.setOnMyLocationClickListener(this)

        poisList.forEach {
            val marker = googleMap.addMarker(
                MarkerOptions()
                    .position(LatLng(it.latitude, it.longitude))
                    .title(it.name + " - " + it.address)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    .alpha(0.7f)
            )
            markers[it.markId] = marker
        }

        liveEventsList.forEach {
            val marker = googleMap.addMarker(
                MarkerOptions()
                    .position(LatLng(it.latitude, it.longitude))
                    .title(it.name + " - " + it.address)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                    .alpha(0.7f)
            )
            markers[it.id] = marker
        }
    }

    override fun onMapClick(positionOnMap: LatLng) {
        Log.v(TAG, "GoogleMap.OnMapClickListener.onMapClick")
        val address = try {
            geocoder.getFromLocation(positionOnMap.latitude, positionOnMap.longitude, 1)[0]
        } catch (exc: IOException) {
            Log.e(TAG, "Exception thrown while using the geocoder: ${exc.message}")
            null
        }

        val createPoiDialog = address?.let {
            CreatePoiDialogFragment.newInstance(
                positionOnMap.latitude,
                positionOnMap.longitude,
                it.getAddressLine(0),
                it.phone,
                it.url
            )
        } ?: run {
            CreatePoiDialogFragment.newInstance(positionOnMap.latitude, positionOnMap.longitude)
        }

        activity?.let {
            createPoiDialog.show(it.supportFragmentManager, "CreatePoiDialogFragment")
        }
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        Log.v(TAG, "GoogleMap.OnMarkerClickListener.onMarkerClick")
        // Searching for the marker in the map of saved markers.
        val foundMarker = markers.entries.filter { it.value == marker }
        if(foundMarker.isEmpty()) {
            Log.e(TAG, "Marker not found in the map of saved markers.")
        }

        // The marker has been found, hence we need to discover if it is a live event or point of interest.
        val markerId = foundMarker[0].key
        val foundPoi = poisList.filter { it.markId == markerId }
        if(foundPoi.isNotEmpty()) {
            val poiDetailsDialog = PoiDetailsDialogFragment.newInstance(foundPoi[0])

            activity?.let {
                poiDetailsDialog.show(it.supportFragmentManager, "PoiDetailsDialogFragment")
            }
        }
        val foundLiveEvent = liveEventsList.filter { it.id == markerId }
        if(foundLiveEvent.isNotEmpty()) {
            val liveEventDialog = LiveEventDetailsDialogFragment.newInstance(foundLiveEvent[0])

            activity?.let {
                liveEventDialog.show(it.supportFragmentManager, "LiveEventDetailsDialogFragment")
            }
        }

        return false
    }

    override fun onMyLocationClick(location: Location) {
        Log.v(TAG, "GoogleMap.OnMyLocationClickListener.onMyLocationClick")
        TODO("Not yet implemented")
    }

    private fun updateMapUI(supportMapFragment: SupportMapFragment) {
        val locationButtonLayoutResource = Integer.parseInt("1")
        val locationButtonResource = Integer.parseInt("2")

        supportMapFragment.view?.let {
            val layout = it.findViewById<LinearLayout>(locationButtonLayoutResource)
            val locationButton = (layout.parent as View).findViewById<View>(locationButtonResource)
            val layoutParams = locationButton.layoutParams as RelativeLayout.LayoutParams
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0)
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
            layoutParams.setMargins(0, 0, 30, 30)
        }
    }
}