package it.unibo.socialplaces.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import it.unibo.socialplaces.R
import it.unibo.socialplaces.config.PushNotification
import it.unibo.socialplaces.receiver.RecommendationAlarm
import it.unibo.socialplaces.domain.LiveEvents
import it.unibo.socialplaces.domain.PointsOfInterest
import it.unibo.socialplaces.fragment.MainFragment
import it.unibo.socialplaces.fragment.dialog.*
import it.unibo.socialplaces.fragment.dialog.liveevents.LiveEventDetailsDialogFragment
import it.unibo.socialplaces.fragment.dialog.pointsofinterest.CreatePoiOrLiveDialogFragment
import it.unibo.socialplaces.fragment.dialog.pointsofinterest.PoiDetailsDialogFragment
import it.unibo.socialplaces.model.liveevents.AddLiveEvent
import it.unibo.socialplaces.model.liveevents.LiveEvent
import it.unibo.socialplaces.model.pointofinterests.AddPointOfInterest
import it.unibo.socialplaces.model.pointofinterests.AddPointOfInterestPoi
import it.unibo.socialplaces.model.pointofinterests.PointOfInterest
import it.unibo.socialplaces.receiver.GeofenceBroadcastReceiver
import it.unibo.socialplaces.service.LocationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

@SuppressLint("UnspecifiedImmutableFlag")
class MainActivity: AppCompatActivity(R.layout.activity_main),
    LocationService.LocationListener,
    LocationService.GeofenceListener,
    CreatePoiOrLiveDialogFragment.CreatePoiOrLiveDialogListener,
    PoiDetailsDialogFragment.PoiDetailsDialogListener,
    LiveEventDetailsDialogFragment.LiveEventDetailsDialogListener {
    companion object {
        val TAG: String = MainActivity::class.qualifiedName!!
    }

    // App state
    /**
     * Reference to the active [LocationService] which periodically retrieves the current
     * location from the GPS.
     */
    private lateinit var locationService: LocationService


    // Geofence manager
    private lateinit var geofencingClient: GeofencingClient

    // Pending Intent to handle geofence trigger
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
        intent.action = getString(R.string.recommendation_geofence_enter)
        PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    /**
     * Object for connecting with a handler to the active instance of [LocationService].
     * When connecting to the current instance, a reference to this activity is given to it
     * in order to get the method [onLocationChanged] called when the location is updated.
     */
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            Log.d(TAG, "LocationService connected to MainActivity.")
            val binder = service as LocationService.LocationBinder
            locationService = binder.getService()
            locationService.setLocationListener(this@MainActivity)
            locationService.setGeofenceListener(this@MainActivity)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            Log.d(TAG, "LocationService disconnected from MainActivity.")
        }
    }

    /**
     * When `true` it means that the activity was (re)launched via a notification handler,
     * i.e., by one of the activities that are present in the package "handler";
     * when `false` is just a normal run of the activity.
     * @see it.unibo.socialplaces.activity.handler.FriendRequestAcceptedActivity
     * @see it.unibo.socialplaces.activity.handler.LiveEventActivity
     * @see it.unibo.socialplaces.activity.handler.NewFriendRequestActivity
     * @see it.unibo.socialplaces.activity.handler.PlaceRecommendation
     */
    private var launchedWithNotificationHandler: Boolean = false

    /**
     * Convenience method for printing statuses in [requestBackgroundLocationPermissionLauncher],
     * [requestHumanActivityPermissionLauncher], [requestLocationPermissionLauncher].
     */
    private fun printPermissionResult(permission: String, status: Boolean) {
        if(status) {
            Log.i(TAG, "Permission to access $permission GRANTED.")
        } else {
            Log.e(TAG, "Permission to access $permission DENIED.")
        }
    }

    /**
     * Permission request launcher (for read activity permission).
     */
    private val requestHumanActivityPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            isGranted: Boolean -> printPermissionResult("human activity", isGranted)
    }

    /**
     * Permission request launcher (for location permission).
     */
    private val requestLocationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            isGranted: Boolean -> printPermissionResult("location", isGranted)
    }

    /**
     * Permission request launcher (for background location permission).
     */
    private val requestBackgroundLocationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            isGranted: Boolean -> printPermissionResult("background location", isGranted)
    }

    /**
     * Callback for updating the map shown in [MapFragment].
     */
    lateinit var onLocationUpdated: (Location) -> Unit

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "onCreate")
        super.onCreate(savedInstanceState)

        // Loading the notification manager.
        PushNotification.loadNotificationManager(this)

        // First all the permission are checked (one after the other),
        // then the lists of points of interest and live events are loaded.
        checkPermissionsAndFetchData()
    }

    override fun onNewIntent(intent: Intent?) {
        Log.v(TAG, "onNewIntent")
        super.onNewIntent(intent)
        // Since the activity can be started many times but the instance is kept,
        // the intent must be updated every time it is changed.
        setIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(connection)
    }


    override fun onResume() {
        Log.v(TAG, "onResume")
        super.onResume()

        if (!launchedWithNotificationHandler) {
            launchedWithNotificationHandler = intent.getBooleanExtra("notification",false)
            intent.removeExtra("notification")
        }
        CoroutineScope(Dispatchers.IO).launch {
            updateGeofence(PointsOfInterest.getPointsOfInterest())
        }
        Log.i(TAG, "${if(launchedWithNotificationHandler) "Launched" else "NOT launched"} by a notification.")
        if(launchedWithNotificationHandler) {
            Log.d(TAG, "Notification found, hence the MainFragment is pushed.")
            CoroutineScope(Dispatchers.IO).launch {
                val poisList = PointsOfInterest.getPointsOfInterest()
                val leList = LiveEvents.getLiveEvents()

                val mainFragment = buildMainFragment(poisList, leList,launchedWithNotificationHandler)
                launchedWithNotificationHandler = false
                CoroutineScope(Dispatchers.Main).launch {
                    supportFragmentManager.beginTransaction().apply {
                        replace(R.id.main_fragment, mainFragment)
                        setReorderingAllowed(true)
                        commit()
                    }
                }
            }
        } else {
            Log.d(TAG, "NO notification found, hence the MainFragment is NOT pushed.")
        }
    }

    /**
     * Fetches the points of interest from the SocialPlaces API and loads the [MainFragment]
     */
    private fun fetchPoisAndLiveEvents() {
        Log.v(TAG, "fetchPoisAndLiveEvents")
        if (!launchedWithNotificationHandler) {
            launchedWithNotificationHandler = intent.getBooleanExtra("notification",false)
            intent.removeExtra("notification")
        }

        val handlingNotificationInFetching = launchedWithNotificationHandler
        launchedWithNotificationHandler = false

        CoroutineScope(Dispatchers.IO).launch {

            val poisList = PointsOfInterest.getPointsOfInterest(forceSync = true)
            updateGeofence(poisList)
            val leList = LiveEvents.getLiveEvents(forceSync = true)

            val mainFragment = buildMainFragment(poisList, leList,handlingNotificationInFetching)
            CoroutineScope(Dispatchers.Main).launch {
                supportFragmentManager.beginTransaction().apply {
                    replace(R.id.main_fragment, mainFragment)
                    setReorderingAllowed(true)
                    commit()
                }
            }

        }
    }

    /**
     * Creates an instance of [MainFragment] taking in consideration the value of
     * [launchedWithNotificationHandler] and [intent.action].
     * Initializes [onLocationUpdated] and then sets [launchedWithNotificationHandler] to `false`.
     * @return [MainFragment] instance
     */
    private fun buildMainFragment(poisList: List<PointOfInterest>, leList: List<LiveEvent>,handlingNotificationInFetching: Boolean): MainFragment {
        val mainFragment = if(handlingNotificationInFetching) {
            when (intent.action) {
                getString(R.string.activity_place_place_recommendation) -> {
                    Log.i(TAG, "Handling a notification with a Point of Interest recommendation.")
                    val poi: PointOfInterest = intent.getParcelableExtra("place")!!
                    MainFragment.newInstance(poisList, leList, poi, getString(R.string.activity_place_place_recommendation))
                }
                getString(R.string.activity_place_validity_recommendation) -> {
                    Log.i(TAG, "Handling a notification with a validity Point of Interest recommendation.")
                    val poi: PointOfInterest = intent.getParcelableExtra("place")!!
                    MainFragment.newInstance(poisList, leList, poi, getString(R.string.activity_place_validity_recommendation))
                }
                getString(R.string.activity_new_live_event) -> {
                    Log.i(TAG, "Handling a notification with a new Live Event creation.")
                    val live: LiveEvent = intent.getParcelableExtra("liveEvent")!!
                    MainFragment.newInstance(poisList, leList, live)
                }
                getString(R.string.activity_friend_request_accepted) -> {
                    Log.i(TAG, "Handling a notification with a friend request accepted.")
                    val friend = intent.getStringExtra("friendUsername")!!
                    MainFragment.newInstance(poisList, leList, friend,false)
                }
                getString(R.string.activity_new_friend_request) -> {
                    Log.i(TAG, "Handling a notification with a new friend request.")
                    val friend = intent.getStringExtra("friendUsername")!!
                    MainFragment.newInstance(poisList, leList, friend,true)
                }
                else -> {
                    Log.e(TAG, "Handling a strange behaviour: intent.action = ${intent.action} not recognized.")
                    MainFragment.newInstance(poisList, leList)
                }
            }
        } else {
            Log.i(TAG, "No notifications found. Do you think is a normal behaviour?")
            MainFragment.newInstance(poisList, leList)
        }

        onLocationUpdated = mainFragment::onCurrentLocationUpdated

        return mainFragment
    }

    /**
     * Calls the methods for checking permissions and then loading background services and fetching
     * points of interest and live events.
     */
    private fun checkPermissionsAndFetchData() {
        checkActivityPermission {
            checkLocationPermissions {
                checkBackgroundLocationPermissions {
                    startLocationService()
                    startAlarmService()
                    fetchPoisAndLiveEvents()
                }
            }
        }
    }

    /**
     * This method checks permissions to ACTIVITY_RECOGNITION.
     */
    private fun checkActivityPermission(next: () -> Unit) {
        Log.v(TAG, "checkActivityPermission")
        // Location permission check
        when {
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED -> {
                Log.i(TAG, "Enabling activity services since permissions were given.")
                next()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) -> {
                Log.i(TAG, "Asking the user for activity recognition (rationale).")
                // addition rationale should be displayed
                val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                builder.setTitle(title)
                    .setMessage(R.string.activity_permission_required)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        requestHumanActivityPermissionLauncher.launch(
                            Manifest.permission.ACTIVITY_RECOGNITION
                        )
                    } // The user can only accept location functionalities.
                builder.create().show()
            }
            else -> {
                Log.i(TAG, "Asking the user for activity permissions.")
                requestHumanActivityPermissionLauncher.launch(
                    Manifest.permission.ACTIVITY_RECOGNITION
                )
            }
        }
    }

    /**
     * This method checks permissions to ACCESS_FINE_LOCATION.
     */
    private fun checkLocationPermissions(next: () -> Unit)  {
        Log.v(TAG, "checkLocationPermissions")
        // Location permission check
        when {
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                Log.i(TAG, "Enabling location services since permissions were given.")
                next()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                Log.i(TAG, "Asking the user for location permissions (rationale).")
                // addition rationale should be displayed
                val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                builder.setTitle(title)
                    .setMessage(R.string.location_permission_required)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        requestLocationPermissionLauncher.launch(
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    } // The user can only accept location functionalities.
                builder.create().show()
            }
            else -> {
                Log.i(TAG, "Asking the user for location permissions.")
                requestLocationPermissionLauncher.launch(
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
        }
    }

    /**
     * This method checks permissions to ACCESS_BACKGROUND_LOCATION.
     */
    private fun checkBackgroundLocationPermissions(next: () -> Unit) {
        Log.v(TAG, "checkBackgroundLocationPermissions")
        // Background location permission check
        when {
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                Log.i(TAG, "Enabling background location services since permissions were given.")
                next()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                Log.i(TAG, "Asking the user for background location permissions (rationale).")
                // addition rationale should be displayed
                val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                builder.setTitle(title)
                    .setMessage(R.string.background_location_permission_required)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        requestBackgroundLocationPermissionLauncher.launch(
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )
                    } // The user can only accept location functionalities.
                builder.create().show()
            }
            else -> {
                Log.i(TAG, "Asking the user for background location permissions.")
                requestBackgroundLocationPermissionLauncher.launch(
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        Log.v(TAG, "onRequestPermissionsResult")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        val loadServicesAndFetchData: () -> Unit = {
            startLocationService()
            startAlarmService()
            fetchPoisAndLiveEvents()
        }

        val bgLocationAndLoad: () -> Unit = {
            checkBackgroundLocationPermissions { loadServicesAndFetchData() }
        }

        val locationAndLoad: () -> Unit = {
            checkLocationPermissions { bgLocationAndLoad() }
        }

        when(permissions[0]) {
            Manifest.permission.ACTIVITY_RECOGNITION -> {
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationAndLoad()
                } else {
                    finishActivityForNoPermissionGranted(R.string.human_activity_permission_denied)
                }
            }
            Manifest.permission.ACCESS_FINE_LOCATION -> {
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    bgLocationAndLoad()
                } else {
                    finishActivityForNoPermissionGranted(R.string.location_permission_denied)
                }
            }
            Manifest.permission.ACCESS_BACKGROUND_LOCATION -> {
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    loadServicesAndFetchData()
                } else {
                    finishActivityForNoPermissionGranted(R.string.access_background_location_permission_denied)
                }

            }
            else -> Unit
        }
    }

    /**
     * Displays a snackbar for informing the user the application is getting closed since
     * they did not accept the permissions.
     */
    private fun finishActivityForNoPermissionGranted(permissionDenied: Int) {
        val snackbar = Snackbar.make(
            findViewById(android.R.id.content),
            permissionDenied,
            5000
        ).apply {
            setActionTextColor(Color.DKGRAY)
            view.setBackgroundColor(Color.BLACK)

            addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    super.onDismissed(transientBottomBar, event)
                    finish()
                }
            })
        }

        snackbar.show()
    }

    /**
     * Starts an instance of [LocationService] and binds the current activity to it so [locationService]
     * can refer to it.
     */
    private fun startLocationService() {
        Log.v(TAG, "startLocationService")

        val startIntent = Intent(this, LocationService::class.java).apply {
            action = LocationService.START_LOCATION_SERVICE
        }
        startService(startIntent)

        val bindIntent = Intent(this, LocationService::class.java)
        bindService(bindIntent, connection, Context.BIND_AUTO_CREATE)

        Toast.makeText(this, R.string.location_service_started, Toast.LENGTH_SHORT).show()
    }

    /**
     * Retrieves the instance of [AlarmManager] for running [RecommendationAlarm].
     */
    @SuppressLint("UnspecifiedImmutableFlag")
    private fun startAlarmService() {
        Log.v(TAG, "startAlarmService")

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(this, RecommendationAlarm::class.java)
        alarmIntent.action = getString(R.string.recommendation_periodic_alarm)

        val recommendationBroadcast = PendingIntent.getBroadcast(
            this,
            0,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            Clock.System.now().toEpochMilliseconds() + 10, // Only while developing
//            AlarmManager.INTERVAL_HOUR * 3,
            AlarmManager.INTERVAL_HOUR * 3,
            recommendationBroadcast
        )
    }

    /**
     * @see LocationService.LocationListener.onLocationChanged
     */
    override fun onLocationChanged(service: Service, location: Location) {
        if(this::onLocationUpdated.isInitialized) {
            onLocationUpdated(location)
        }
    }
    /**
     * @see LocationService.GeofenceListener.onGeofenceClientCreated
     */
    override fun onGeofenceClientCreated(geofencingClient: GeofencingClient){
        Log.v(TAG,"Connected geofenceClient")
        this.geofencingClient = geofencingClient
        CoroutineScope(Dispatchers.IO).launch {
            val poisList = PointsOfInterest.getPointsOfInterest()
            updateGeofence(poisList)
        }

    }


    /**
     * @see CreatePoiOrLiveDialogFragment.CreatePoiOrLiveDialogListener.onAddLiveEvent
     */
    override fun onAddLiveEvent(dialog: DialogFragment, addLiveEvent: AddLiveEvent) {
        Log.v(TAG, "CreatePoiOrLiveDialogFragment.CreatePoiOrLiveDialogListener.onAddLiveEvent")
        CoroutineScope(Dispatchers.IO).launch {
            LiveEvents.addLiveEvent(addLiveEvent)
            CoroutineScope(Dispatchers.Main).launch {
                dialog.dismiss()
            }
        }
    }

    /**
     * @see CreatePoiOrLiveDialogFragment.CreatePoiOrLiveDialogListener.onAddPointOfInterest
     */
    override fun onAddPointOfInterest(dialog: DialogFragment, addPointOfInterestPoi: AddPointOfInterestPoi) {
        Log.v(TAG, "CreatePoiOrLiveDialogFragment.CreatePoiOrLiveDialogListener.onAddPointOfInterest")
        CoroutineScope(Dispatchers.IO).launch {

            PointsOfInterest.addPointOfInterest(AddPointOfInterest(addPointOfInterestPoi))
            updateGeofence(PointsOfInterest.getPointsOfInterest())

            CoroutineScope(Dispatchers.Main).launch {
                dialog.dismiss()
            }
        }
    }

    private fun buildGeofence(poi: PointOfInterest): GeofencingRequest {
        val geofence = Geofence.Builder()
            .setRequestId(poi.markId)
            .setCircularRegion(
                poi.latitude,
                poi.longitude,
                resources.getInteger(R.integer.GEOFENCE_RADIUS_IN_METERS).toFloat()
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        return GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

    }

    @SuppressLint("MissingPermission")
    private fun addGeofenceListener(geofence: GeofencingRequest){
        
        geofencingClient.removeGeofences(geofencePendingIntent).run {
            addOnCompleteListener {
                geofencingClient.addGeofences(geofence, geofencePendingIntent).run {
                    addOnSuccessListener {
                        Log.v(TAG, "Added geofence with id: ${geofence.geofences[0].requestId}")
                    }
                    addOnFailureListener {
                        if ((it.message != null)) {
                            Log.e(TAG, it.message!!)
                        }
                    }
                }
            }
        }
    }

    private fun createGeofence(poi: PointOfInterest){
        val geofence = buildGeofence(poi)
        Log.v(TAG,"Geofence created $geofence ")
        addGeofenceListener(geofence)
    }

    private fun updateGeofence(poisList: List<PointOfInterest>){
        Log.v(TAG,"Updating geofence ${this::geofencingClient.isInitialized}")
        //Create geofence
        if(this::geofencingClient.isInitialized){
            for(poi in poisList){
                createGeofence(poi)
            }
        }
    }

    /**
     * @see PoiDetailsDialogFragment.PoiDetailsDialogListener.onShareButtonPressed
     */
    override fun onShareButtonPressed(dialog: DialogFragment, poi: PointOfInterest) {
        Log.v(TAG, "PoiDetailsDialogFragment.PoiDetailsDialogListener.onShareButtonPressed")
        sharePlace(poi.name, poi.address, poi.latitude, poi.longitude)
    }

    /**
     * @see LiveEventDetailsDialogFragment.LiveEventDetailsDialogListener.onShareButtonPressed
     */
    override fun onShareButtonPressed(dialog: DialogFragment, liveEvent: LiveEvent) {
        Log.v(TAG, "LiveEventDetailsDialogFragment.onShareButtonPressed")
        sharePlace(liveEvent.name, liveEvent.address, liveEvent.latitude, liveEvent.longitude)
    }

    /**
     * Convenience method for sharing details about a poi or live in [onShareButtonPressed].
     */
    private fun sharePlace(name: String, address: String, latitude: Double, longitude: Double) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, getString(R.string.share_place, name, address, latitude, longitude))
        }
        val createdIntent = Intent.createChooser(
            shareIntent,
            getString(R.string.share_place_intent, name)
        )
        ContextCompat.startActivity(this, createdIntent, null)
    }

    /**
     * @see PoiDetailsDialogFragment.PoiDetailsDialogListener.onRouteButtonPressed
     * @see LiveEventDetailsDialogFragment.LiveEventDetailsDialogListener.onRouteButtonPressed
     */
    override fun onRouteButtonPressed(dialog: DialogFragment, address: String) {
        Log.v(TAG, "Poi/LiveDetailsDialogFragment.Poi/LiveDetailsDialogListener.onRouteButtonPressed")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$address"))
        dialog.dismiss()
        startActivity(intent)
    }
}
