package com.example.maptry.fragment

import android.annotation.SuppressLint
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.example.maptry.R
import com.example.maptry.ui.CircleTransform
import com.example.maptry.config.Auth
import com.example.maptry.databinding.FragmentMainBinding
import com.example.maptry.domain.LiveEvents
import com.example.maptry.domain.PointsOfInterest
import com.example.maptry.fragment.dialog.pointsofinterest.CreatePoiOrLiveDialogFragment
import com.example.maptry.fragment.dialog.liveevents.LiveEventDetailsDialogFragment
import com.example.maptry.fragment.dialog.pointsofinterest.PoiDetailsDialogFragment
import com.example.maptry.model.liveevents.LiveEvent
import com.example.maptry.model.pointofinterests.PointOfInterest
import com.google.android.gms.common.api.Status
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException

class MainFragment : Fragment(R.layout.fragment_main),
    OnMapReadyCallback,
    GoogleMap.OnMapClickListener,
    GoogleMap.OnMarkerClickListener {
    // UI
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    // App state
    private lateinit var poisList: MutableList<PointOfInterest>
    private lateinit var liveEventsList: MutableList<LiveEvent>
    private val markers: MutableMap<String, Marker> = emptyMap<String, Marker>().toMutableMap()
    private lateinit var currentPositionMarker: Marker

    // API
    private val geocoder by lazy {
        Geocoder(this.requireContext())
    }
    private lateinit var map: GoogleMap

    companion object {
        private val TAG = MainFragment::class.qualifiedName

        private const val ARG_POISLIST = "poisList"
        private const val ARG_LIVEEVENTSLIST = "liveEventsList"

        @JvmStatic
        fun newInstance(poisList: List<PointOfInterest>, liveEventsList: List<LiveEvent>) =
            MainFragment().apply {
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

        Places.initialize(requireContext(), getString(R.string.places_api))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.v(TAG, "onViewCreated")
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentMainBinding.bind(view)

        val supportMapFragment = childFragmentManager.findFragmentById(binding.googleMaps.id) as SupportMapFragment
        updateMapUI(supportMapFragment)
        supportMapFragment.getMapAsync(this)
        val autoCompleteFragment = childFragmentManager.findFragmentById(R.id.places_search_bar) as AutocompleteSupportFragment
        autoCompleteFragment.setPlaceFields(listOf(Place.Field.ID, Place.Field.NAME))

        // Set up a PlaceSelectionListener to handle the response.
        autoCompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(p0: Place) {
                // TODO: Get info about the selected place.
                Log.i(TAG, "Place: ${p0.name}, ${p0.id}")
            }
            override fun onError(status: Status) {
                // TODO: Handle the error.
                Log.i(TAG, "An error occurred: $status")
            }
        })
        autoCompleteFragment.view?.let {
            val layout = it as LinearLayout
            val menuIcon = layout.getChildAt(0) as ImageView

            Picasso.get()
                .load(Auth.getUserProfileIcon())
                .transform(CircleTransform())
                .resize(140, 140)
                .into(menuIcon)

            layout.background = ContextCompat.getDrawable(this.requireContext(), R.drawable.layout_bg)

            activity?.let { a ->
                Log.d(TAG, "autoCompleteFragment.view exists.")
                menuIcon.setOnClickListener {
                    val mainMenuFragment = MainMenuFragment()
                    a.supportFragmentManager.beginTransaction().apply {
                        replace(R.id.main_menu_fragment, mainMenuFragment)
                        setReorderingAllowed(true)
                        addToBackStack("MainMenuFragment")
                        commit()
                    }
                }
            }
        } ?: run {
            Log.e(TAG, "autoCompleteFragment.view is null!")
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
        map = googleMap
        map.setOnMarkerClickListener(this)
        map.setOnMapClickListener(this)

        poisList.forEach {
            createMarker(it.latitude,it.longitude,it.name,it.address,it.markId)
        }

        liveEventsList.forEach {
            createMarker(it.latitude,it.longitude,it.name,it.address,it.id,true)
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
            Log.v(TAG,"Address found:")
            println(it)
            CreatePoiOrLiveDialogFragment.newInstance(
                positionOnMap.latitude,
                positionOnMap.longitude,
                it.getAddressLine(0),
                it.phone,
                it.url
            )
        } ?: run {
            CreatePoiOrLiveDialogFragment.newInstance(positionOnMap.latitude, positionOnMap.longitude)
        }

        activity?.let {
            createPoiDialog.show(it.supportFragmentManager, "CreatePoiOrLiveDialogFragment")
        }
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        Log.v(TAG, "GoogleMap.OnMarkerClickListener.onMarkerClick")
        // Searching for the marker in the map of saved markers.
        val foundMarker = markers.entries.filter { it.value == marker }
        if(foundMarker.isNotEmpty()) {
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
        }

        return false
    }

    fun onCurrentLocationUpdated(location: Location) {
        Log.v(TAG, "Current loc $location")

        if(!this::map.isInitialized) {
            Log.w(TAG, "The map from Google Maps has not been initialized yet. The map cannot update its current position.")
            return
        }
        val currentPosition = LatLng(location.latitude, location.longitude)
        if(this::currentPositionMarker.isInitialized) {
            if(currentPositionMarker.position == currentPosition) {
                return
            }
            currentPositionMarker.remove()
        }
        drawCurrentPositionMarker(currentPosition)

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentPosition, 17F))
    }

    private fun drawCurrentPositionMarker(currentPosition: LatLng) {
        val markerIcon = ContextCompat.getDrawable(requireContext(), R.mipmap.ic_launcher_foreground)
        markerIcon?.toBitmap(150, 150)?.let { ic ->
            map.addMarker(
                MarkerOptions()
                    .position(currentPosition)
                    .icon(BitmapDescriptorFactory.fromBitmap(ic))
                    .alpha(1f)
            )?.let {
                currentPositionMarker = it
            }
        }
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

    private fun createMarker(latitude: Double, longitude: Double, name: String, address: String, id: String, isLive: Boolean=false) {
        val color = if(isLive) BitmapDescriptorFactory.HUE_GREEN else BitmapDescriptorFactory.HUE_RED

        val marker = map.addMarker(
            MarkerOptions()
                .position(LatLng(latitude, longitude))
                .title("$name - $address")
                .icon(BitmapDescriptorFactory.defaultMarker(color))
                .alpha(0.7f)
        )
        marker?.let { markers[id] = it }
    }

    override fun onResume() {
        Log.v(TAG, "onResume")
        super.onResume()

        PointsOfInterest.setCreateMarkerCallback { lat, lon, name, addr, id, isLive ->
            CoroutineScope(Dispatchers.Main).launch {
                createMarker(lat, lon, name, addr, id, isLive)
            }
        }
        LiveEvents.setCreateMarkerCallback { lat, lon, name, addr, id, isLive ->
            CoroutineScope(Dispatchers.Main).launch {
                createMarker(lat, lon, name, addr, id, isLive)
            }
        }
        PointsOfInterest.setUpdatePoiCallback(this::updatePoiAndLive)
        LiveEvents.setUpdateLiveCallback(this::updatePoiAndLive)

        if(this::map.isInitialized) {
            Log.i(TAG, "Clearing the map from previously added markers, re-adding them.")
            map.clear()

            updatePoiAndLive()

            CoroutineScope(Dispatchers.Main).launch {
                drawCurrentPositionMarker(currentPositionMarker.position)
                poisList.forEach {
                    createMarker(it.latitude, it.longitude, it.name, it.address, it.markId)
                }
                liveEventsList.forEach {
                    createMarker(it.latitude, it.longitude, it.name, it.address, it.id, true)
                }
            }
        }
    }

    private fun updatePoiAndLive() {
        CoroutineScope(Dispatchers.IO).launch {
            poisList.clear()
            liveEventsList.clear()

            liveEventsList.addAll(LiveEvents.getLiveEvents())
            poisList.addAll(PointsOfInterest.getPointsOfInterest())

            arguments?.apply {
                putParcelableArray(ARG_POISLIST, poisList.toTypedArray())
                putParcelableArray(ARG_LIVEEVENTSLIST, liveEventsList.toTypedArray())
            }
        }
    }

    override fun onDestroy() {
        Log.v(TAG, "onDestroy")
        super.onDestroy()

        PointsOfInterest.disableCallback()
        LiveEvents.disableCallback()
    }
}