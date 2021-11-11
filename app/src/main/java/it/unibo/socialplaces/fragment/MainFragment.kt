package it.unibo.socialplaces.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.core.app.ActivityCompat
import it.unibo.socialplaces.R
import it.unibo.socialplaces.ui.CircleTransform
import it.unibo.socialplaces.config.Auth
import it.unibo.socialplaces.databinding.FragmentMainBinding
import it.unibo.socialplaces.domain.LiveEvents
import it.unibo.socialplaces.domain.PointsOfInterest
import it.unibo.socialplaces.fragment.dialog.pointsofinterest.CreatePoiOrLiveDialogFragment
import it.unibo.socialplaces.fragment.dialog.liveevents.LiveEventDetailsDialogFragment
import it.unibo.socialplaces.fragment.dialog.pointsofinterest.PoiDetailsDialogFragment
import it.unibo.socialplaces.model.liveevents.LiveEvent
import it.unibo.socialplaces.model.pointofinterests.PointOfInterest
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
    private val markerColors = mapOf(
        "restaurants" to BitmapDescriptorFactory.HUE_ORANGE,
        "leisure" to BitmapDescriptorFactory.HUE_GREEN,
        "sport" to BitmapDescriptorFactory.HUE_BLUE,
        "live" to BitmapDescriptorFactory.HUE_YELLOW
    )

    // App state
    private lateinit var poisList: MutableList<PointOfInterest>
    private lateinit var liveEventsList: MutableList<LiveEvent>
    private val markers: MutableMap<String, Marker> = emptyMap<String, Marker>().toMutableMap()

    private var notificationPoi: PointOfInterest? = null
    private var notificationLive: LiveEvent? = null
    private var friendUsername: String? = null
    private lateinit var currentLatLng: LatLng
    private var isShowingDetails: Boolean = false

    // API
    private val geocoder by lazy {
        Geocoder(this.requireContext())
    }
    private lateinit var map: GoogleMap

    companion object {
        private val TAG = MainFragment::class.qualifiedName

        private const val ARG_POISLIST = "poisList"
        private const val ARG_LIVEEVENTSLIST = "liveEventsList"
        private const val ARG_NOTIFICATIONPOI = "notificationPoi"
        private const val ARG_NOTIFICATIONLIVE = "notificationLive"
        private const val ARG_NOTIFICATIONFRIEND = "notificationFriend"

        @JvmStatic
        fun newInstance(
            poisList: List<PointOfInterest>,
            liveEventsList: List<LiveEvent>,
            poi: PointOfInterest?
        ) =
            newInstance(poisList,liveEventsList).apply {
                arguments?.apply {
                    if (poi != null){
                        putParcelable(ARG_NOTIFICATIONPOI,poi)
                    }
                }
            }

        @JvmStatic
        fun newInstance(
            poisList: List<PointOfInterest>,
            liveEventsList: List<LiveEvent>,
            live: LiveEvent?
        ) =
            newInstance(poisList,liveEventsList).apply {
                arguments?.apply {
                    if (live != null){
                        putParcelable(ARG_NOTIFICATIONLIVE,live)
                    }
                }
            }

        @JvmStatic
        fun newInstance(
            poisList: List<PointOfInterest>,
            liveEventsList: List<LiveEvent>,
            friendUsername: String?
        ) =
            newInstance(poisList,liveEventsList).apply {
                arguments?.apply {
                    if (friendUsername != null){
                        putString(ARG_NOTIFICATIONFRIEND,friendUsername)
                    }
                }
            }

        @JvmStatic
        fun newInstance(
            poisList: List<PointOfInterest>,
            liveEventsList: List<LiveEvent>
        ) =
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

            notificationPoi = it.getParcelable(ARG_NOTIFICATIONPOI)
            notificationLive = it.getParcelable(ARG_NOTIFICATIONLIVE)
            friendUsername = it.getString(ARG_NOTIFICATIONFRIEND)
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
            val userIconUri = Auth.getUserProfileIcon()
            if(userIconUri != null) {
                Picasso.get()
                    .load(userIconUri)
                    .transform(CircleTransform())
                    .resize(140, 140)
                    .into(menuIcon)
                layout.setBackgroundResource(R.drawable.layout_bg)
            }

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
        if (
            ActivityCompat.checkSelfPermission(this.requireContext(),Manifest.permission.ACCESS_FINE_LOCATION)
            ==
            PackageManager.PERMISSION_GRANTED
        ) {
            map.isMyLocationEnabled = true
        }

        poisList.forEach {
            createMarker(it.latitude,it.longitude,it.name,it.address,it.markId,it.type)
        }

        liveEventsList.forEach {
            createMarker(it.latitude,it.longitude,it.name,it.address,it.id,"live")
        }

        notificationPoi?.let {
            val shouldCreateMarker = !poisList.contains(it)
            showNotifiedPoiOrLiveOnMap(it.latitude,it.longitude,it.name,it.address,it.markId,it.type,shouldCreateMarker)
            notificationPoi = null
        }

        notificationLive?.let {
            val shouldCreateMarker = !liveEventsList.contains(it)
            showNotifiedPoiOrLiveOnMap(it.latitude,it.longitude,it.name,it.address,it.id,"live",shouldCreateMarker)
            notificationLive = null
        }

        friendUsername?.let {
            friendUsername = null
        }
    }

    private fun showNotifiedPoiOrLiveOnMap(latitude: Double, longitude: Double, name: String, address: String, id: String, type: String, shouldCreateMarker: Boolean){
        if(shouldCreateMarker) {
            createMarker(latitude, longitude, name, address, id, type)
        }

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(latitude,longitude), 17F))
        markers[id]?.let { onMarkerClick(it) }
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
        isShowingDetails = true
        // Searching for the marker in the map of saved markers.
        val foundMarker = markers.entries.filter { it.value == marker }
        if(foundMarker.isNotEmpty()) {
            // The marker has been found, hence we need to discover if it is a live event or point of interest.
            val markerId = foundMarker[0].key

            val foundPoi = poisList.filter { it.markId == markerId }
            if(foundPoi.isNotEmpty()) {
                val poiDetailFragment = PoiDetailsDialogFragment.newInstance(foundPoi[0])
                poiDetailFragment.setOnDismissCallback { isShowingDetails = false }

                activity?.let {
                    poiDetailFragment.show(it.supportFragmentManager, "PoiDetailsDialogFragment")
                }
            }

            val foundLiveEvent = liveEventsList.filter { it.id == markerId }
            if(foundLiveEvent.isNotEmpty()) {
                val liveDetailFragment = LiveEventDetailsDialogFragment.newInstance(foundLiveEvent[0])
                liveDetailFragment.setOnDismissCallback { isShowingDetails = false }

                activity?.let {
                    liveDetailFragment.show(it.supportFragmentManager, "LiveEventDetailsDialogFragment")
                }
            }
        }

        return false
    }

    fun onCurrentLocationUpdated(location: Location) {
        if(!this::map.isInitialized) {
            Log.w(TAG, "The map from Google Maps has not been initialized yet. The map cannot update its current position.")
            return
        }
        val newPosition = LatLng(location.latitude, location.longitude)

        if(this::currentLatLng.isInitialized && checkIfPositionsAreEqualApproximated(newPosition, currentLatLng)) {
            return
        }

        if(!isShowingDetails){
            currentLatLng = newPosition
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(newPosition, 17F))
        }
    }

    private fun checkIfPositionsAreEqualApproximated(pos0: LatLng, pos1: LatLng): Boolean {
        val lat0 = "%.4f".format(pos0.latitude).toDouble()
        val lon0 = "%.4f".format(pos0.longitude).toDouble()
        val lat1 = "%.4f".format(pos1.latitude).toDouble()
        val lon1 = "%.4f".format(pos1.longitude).toDouble()

        return lat0 == lat1 && lon0 == lon1
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

    private fun createMarker(latitude: Double, longitude: Double, name: String, address: String, id: String, type: String) {
        val color = markerColors[type.lowercase()] ?: BitmapDescriptorFactory.HUE_RED

        val marker = map.addMarker(
            MarkerOptions()
                .position(LatLng(latitude, longitude))
                .title(name)
                .icon(BitmapDescriptorFactory.defaultMarker(color))
                .snippet(address)
                .alpha(0.7f)
        )

        marker?.let { markers[id] = it }
    }

    override fun onResume() {
        Log.v(TAG, "onResume")
        super.onResume()

        PointsOfInterest.setCreateMarkerCallback { lat, lon, name, addr, id, type ->
            CoroutineScope(Dispatchers.Main).launch {
                createMarker(lat, lon, name, addr, id, type)
            }
        }
        LiveEvents.setCreateMarkerCallback { lat, lon, name, addr, id, _ ->
            CoroutineScope(Dispatchers.Main).launch {
                createMarker(lat, lon, name, addr, id, "live")
            }
        }
        PointsOfInterest.setUpdatePoiCallback(this::updatePoiAndLive)
        LiveEvents.setUpdateLiveCallback(this::updatePoiAndLive)

        if(this::map.isInitialized) {
            Log.i(TAG, "Clearing the map from previously added markers, re-adding them.")
            map.clear()

            updatePoiAndLive {
                CoroutineScope(Dispatchers.Main).launch {
                    poisList.forEach {
                        createMarker(it.latitude, it.longitude, it.name, it.address, it.markId, it.type)
                    }
                    liveEventsList.forEach {
                        createMarker(it.latitude, it.longitude, it.name, it.address, it.id, "live")
                    }

                    notificationPoi?.let {
                        val shouldCreateMarker = !poisList.contains(it)
                        showNotifiedPoiOrLiveOnMap(it.latitude,it.longitude,it.name,it.address,it.markId,it.type,shouldCreateMarker)
                        notificationPoi = null
                    }

                    notificationLive?.let {
                        val shouldCreateMarker = !liveEventsList.contains(it)
                        showNotifiedPoiOrLiveOnMap(it.latitude,it.longitude,it.name,it.address,it.id,"live",shouldCreateMarker)
                        notificationLive = null
                    }

                    friendUsername?.let {
                        friendUsername = null
                    }
                }
            }
        }
    }

    private fun updatePoiAndLive(thenCallback: () -> Unit = {}) {
        CoroutineScope(Dispatchers.IO).launch {
            poisList.clear()
            liveEventsList.clear()

            liveEventsList.addAll(LiveEvents.getLiveEvents())
            poisList.addAll(PointsOfInterest.getPointsOfInterest())

            arguments?.apply {
                putParcelableArray(ARG_POISLIST, poisList.toTypedArray())
                putParcelableArray(ARG_LIVEEVENTSLIST, liveEventsList.toTypedArray())
            }
        }.invokeOnCompletion {
            thenCallback()
        }
    }

    override fun onDestroy() {
        Log.v(TAG, "onDestroy")
        super.onDestroy()

        PointsOfInterest.disableCallback()
        LiveEvents.disableCallback()
    }
}