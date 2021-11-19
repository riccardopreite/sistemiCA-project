package it.unibo.socialplaces.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
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
import it.unibo.socialplaces.activity.list.FriendsListActivity
import it.unibo.socialplaces.fragment.dialog.handler.FriendRequestDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException


class MainFragment : Fragment(R.layout.fragment_main),
    OnMapReadyCallback,
    GoogleMap.OnMapClickListener,
    GoogleMap.OnMarkerClickListener {
    /**
     * Maps the types of points of interest / live events to map marker colors.
     */
    private val markerColors = mapOf(
        "restaurants" to BitmapDescriptorFactory.HUE_ORANGE,
        "leisure" to BitmapDescriptorFactory.HUE_GREEN,
        "sport" to BitmapDescriptorFactory.HUE_BLUE,
        "live" to BitmapDescriptorFactory.HUE_YELLOW
    )

    // UI
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    // App state
    private lateinit var poisList: List<PointOfInterest>
    private lateinit var liveEventsList: List<LiveEvent>

    /**
     * Marker storage (all markers to be visualized in the Google Maps fragment).
     */
    private val markers: MutableMap<String, Marker> = emptyMap<String, Marker>().toMutableMap()

    private var notificationPoi: PointOfInterest? = null
    private var notificationLive: LiveEvent? = null
    private var notificationFriendUsername: String? = null

    /**
     *  Flag controlling whether [notificationFriendUsername] should be used for displaying [FriendsListActivity]
     *  or [FriendRequestDialogFragment].
     */
    private var isFriendshipRequest: Boolean = false
    private lateinit var currentLatLng: LatLng

    /**
     * If `true` then the location on the map UI is not updated, othwerwise it is.
     */
    private var isShowingDetails: Boolean = false

    // API
    /**
     * Geocoder to get marker addresses.
     */
    private val geocoder by lazy { Geocoder(this.requireContext()) }

    /**
     * The map UI in which to paint markers.
     */
    private lateinit var map: GoogleMap

    companion object {
        private val TAG = MainFragment::class.qualifiedName

        private const val ARG_POISLIST = "poisList"
        private const val ARG_LIVEEVENTSLIST = "liveEventsList"
        private const val ARG_NOTIFICATIONPOI = "notificationPoi"
        private const val ARG_NOTIFICATIONLIVE = "notificationLive"
        private const val ARG_NOTIFICATIONFRIEND = "notificationFriend"
        private const val ARG_NOTIFICATIONISFRIENDREQUEST = "notificationFriendRequest"

        /**
         * Creates a new instance of [MainFragment] to be used in the default scenario.
         */
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

        /**
         * Creates a new instance of [MainFragment] to be used when a place recommendation notification
         * is published.
         */
        @JvmStatic
        fun newInstance(
            poisList: List<PointOfInterest>,
            liveEventsList: List<LiveEvent>,
            poi: PointOfInterest?
        ) =
            newInstance(poisList,liveEventsList).apply {
                arguments?.apply {
                    putParcelable(ARG_NOTIFICATIONPOI, poi)
                }
            }

        /**
         * Creates a new instance of [MainFragment] to be used when a new live event notification
         * is published.
         */
        @JvmStatic
        fun newInstance(
            poisList: List<PointOfInterest>,
            liveEventsList: List<LiveEvent>,
            live: LiveEvent?
        ) =
            newInstance(poisList,liveEventsList).apply {
                arguments?.apply {
                    putParcelable(ARG_NOTIFICATIONLIVE, live)
                }
            }

        /**
         * Creates a new instance of [MainFragment] to be used when a new friend request/friend request
         * accepted notification is published.
         */
        @JvmStatic
        fun newInstance(
            poisList: List<PointOfInterest>,
            liveEventsList: List<LiveEvent>,
            friendUsername: String?,
            isFriendshipRequest: Boolean
        ) =
            newInstance(poisList, liveEventsList).apply {
                arguments?.apply {
                    putString(ARG_NOTIFICATIONFRIEND, friendUsername)
                    putBoolean(ARG_NOTIFICATIONISFRIENDREQUEST, isFriendshipRequest)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        arguments?.let {
            val pArrayPois = it.getParcelableArray(ARG_POISLIST)
            poisList = pArrayPois?.let { p ->
                Log.d(TAG, "Loading poisList from savedInstanceState")
                List(p.size) { i -> p[i] as PointOfInterest }
            } ?: run {
                Log.e(TAG, "poisList inside savedInstanceState was null. Loading an emptyList.")
                emptyList()
            }

            val pArrayLive = it.getParcelableArray(ARG_LIVEEVENTSLIST)
            liveEventsList = pArrayLive?.let { p ->
                Log.d(TAG, "Loading liveEventsList from savedInstanceState")
                List(p.size) { i -> p[i] as LiveEvent }
            } ?: run {
                Log.e(TAG, "liveEventsList inside savedInstanceState was null. Loading an emptyList.")
                emptyList()
            }

            notificationPoi = it.getParcelable(ARG_NOTIFICATIONPOI)
            notificationLive = it.getParcelable(ARG_NOTIFICATIONLIVE)
            notificationFriendUsername = it.getString(ARG_NOTIFICATIONFRIEND)
            isFriendshipRequest = it.getBoolean(ARG_NOTIFICATIONISFRIENDREQUEST)
        }

        Places.initialize(requireContext(), getString(R.string.places_api))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.v(TAG, "onViewCreated")
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentMainBinding.bind(view)

        // Loading the Google Maps fragment.
        val supportMapFragment = childFragmentManager.findFragmentById(binding.googleMaps.id) as SupportMapFragment
        updateMapUI(supportMapFragment)
        supportMapFragment.getMapAsync(this)

        // Loading the Places API search bar.
        val autoCompleteFragment = childFragmentManager.findFragmentById(R.id.places_search_bar) as AutocompleteSupportFragment
        autoCompleteFragment.setPlaceFields(listOf(Place.Field.ID, Place.Field.NAME))

        // Set up a PlaceSelectionListener to handle the response.
        autoCompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(p0: Place) {
                // TODO: Get location about the selected place and display it on the map.
                Log.i(TAG, "Place: ${p0.name}, ${p0.id}")
            }
            override fun onError(status: Status) {
                Log.e(TAG, "An error occurred: $status")
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
        map.apply {
            setOnMarkerClickListener(this@MainFragment)
            setOnMapClickListener(this@MainFragment)
            isMyLocationEnabled = (
                ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            )
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

        notificationFriendUsername?.let {
            if(isFriendshipRequest) {
                val friendRequestDialog = FriendRequestDialogFragment.newInstance(it)
                activity?.let { a ->
                    friendRequestDialog.show(a.supportFragmentManager, "FriendRequestDialogFragment")
                }
            } else {
                startActivity(Intent(context, FriendsListActivity::class.java))
            }
        }
    }

    /**
     * Displays a marker in the Google Maps UI.
     */
    private fun showNotifiedPoiOrLiveOnMap(latitude: Double, longitude: Double, name: String, address: String, id: String, type: String, shouldCreateMarker: Boolean) {
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
            Log.i(TAG, "Address has been found, IN-CRE-DI-BLE!!! It is $address.")
            CreatePoiOrLiveDialogFragment.newInstance(
                positionOnMap.latitude,
                positionOnMap.longitude,
                it.getAddressLine(0),
                it.phone,
                it.url
            )
        } ?: run {
            Log.i(TAG, "Address has NOT been found, not surprising...")
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

    /**
     * Callback to be invoked when the current location has been updated.
     * @param location the new location of the user.
     */
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

    /**
     * Approximate location check.
     * First [pos0] and [pos1] are approximated to the 4th decimal digit and then compared.
     *
     * @param pos0 the first location
     * @param pos1 the second location
     * @return `true` if the approximated position are equal, `false` otherwise.
     */
    private fun checkIfPositionsAreEqualApproximated(pos0: LatLng, pos1: LatLng): Boolean {
        val lat0 = "%.4f".format(pos0.latitude).toDouble()
        val lon0 = "%.4f".format(pos0.longitude).toDouble()
        val lat1 = "%.4f".format(pos1.latitude).toDouble()
        val lon1 = "%.4f".format(pos1.longitude).toDouble()

        return lat0 == lat1 && lon0 == lon1
    }

    /**
     * Force tweak the Google Maps fragment UI.
     */
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

    /**
     * Displays a marker in the Google Maps fragment.
     */
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
                    // Default behavior
                    poisList.forEach {
                        createMarker(it.latitude, it.longitude, it.name, it.address, it.markId, it.type)
                    }
                    liveEventsList.forEach {
                        createMarker(it.latitude, it.longitude, it.name, it.address, it.id, "live")
                    }

                    // If the MainFragment has been pushed for handling a place recommendation notification.
                    notificationPoi?.let {
                        val shouldCreateMarker = !poisList.contains(it)
                        showNotifiedPoiOrLiveOnMap(it.latitude,it.longitude,it.name,it.address,it.markId,it.type,shouldCreateMarker)
                        notificationPoi = null
                    }

                    // If the MainFragment has been pushed for handling a new live event notification.
                    notificationLive?.let {
                        val shouldCreateMarker = !liveEventsList.contains(it)
                        showNotifiedPoiOrLiveOnMap(it.latitude,it.longitude,it.name,it.address,it.id,"live",shouldCreateMarker)
                        notificationLive = null
                    }

                    // If the MainFragment has been pushed for handling an new friend request/accepted friend request notification.
                    notificationFriendUsername?.let {
                        notificationFriendUsername = null
                    }
                }
            }
        }
    }

    /**
     * Retrieves the cached live events and points of interest.
     * After having done that, they get saved in the bundle and later [thenCallback] is invoked.
     * @param thenCallback function to be invoked after the data from the cache is retrieved.
     */
    private fun updatePoiAndLive(thenCallback: () -> Unit = {}) {
        CoroutineScope(Dispatchers.IO).launch {
            liveEventsList = LiveEvents.getLiveEvents()
            poisList = PointsOfInterest.getPointsOfInterest()

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

        PointsOfInterest.disableCallbacks()
        LiveEvents.disableCallbacks()
    }
}