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
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import it.unibo.socialplaces.R
import it.unibo.socialplaces.config.Alarm
import it.unibo.socialplaces.config.PushNotification
import it.unibo.socialplaces.receiver.PlaceRecommendationReceiver
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
import it.unibo.socialplaces.service.BackgroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity: AppCompatActivity(R.layout.activity_main),
    BackgroundService.LocationListener,
    CreatePoiOrLiveDialogFragment.CreatePoiOrLiveDialogListener,
    PoiDetailsDialogFragment.PoiDetailsDialogListener,
    LiveEventDetailsDialogFragment.LiveEventDetailsDialogListener {
    companion object {
        val TAG: String = MainActivity::class.qualifiedName!!
    }

    // App state
    /**
     * Reference to the active [BackgroundService] which periodically retrieves the current
     * location from the GPS.
     */
    private var backgroundService: BackgroundService? = null

    /**
     * Object for connecting with a handler to the active instance of [BackgroundService].
     * When connecting to the current instance, a reference to this activity is given to it
     * in order to get the method [onLocationChanged] called when the location is updated.
     */
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            Log.d(TAG, "BackgroundService connected to MainActivity.")
            val binder = service as BackgroundService.LocationBinder
            backgroundService = binder.getService()
            backgroundService?.setLocationListener(this@MainActivity)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            Log.d(TAG, "BackgroundService disconnected from MainActivity.")
            backgroundService = null
        }
    }

    /**
     * When `true` it means that the activity was (re)launched via a notification handler,
     * i.e., by one of the activities that are present in the package "handler";
     * when `false` is just a normal run of the activity.
     * @see it.unibo.socialplaces.activity.handler.FriendRequestAcceptedActivity
     * @see it.unibo.socialplaces.activity.handler.LiveEventActivity
     * @see it.unibo.socialplaces.activity.handler.NewFriendRequestActivity
     * @see it.unibo.socialplaces.activity.handler.PlaceRecommendationActivity
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
     * Callback for updating the map shown in [MainFragment].
     */
    lateinit var onLocationUpdated: (Location) -> Unit

    /**
     * Callback for updating the map location status shown in [MainFragment].
     */
    lateinit var onLocationStatusUpdated: (Boolean) -> Unit

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "onCreate")
        super.onCreate(savedInstanceState)

        // Setting development / production mode
        Alarm.isDevelopment = true

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
            launchedWithNotificationHandler = intent.getBooleanExtra(getString(R.string.extra_notification),false)
            intent.removeExtra(getString(R.string.extra_notification))
        }
        CoroutineScope(Dispatchers.IO).launch {
            backgroundService?.updateGeofences(
                PointsOfInterest.getPointsOfInterest()
                    .map { poi -> Triple(poi.markId, poi.latitude, poi.longitude)}
            )
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
            launchedWithNotificationHandler = intent.getBooleanExtra(getString(R.string.extra_notification),false)
            intent.removeExtra(getString(R.string.extra_notification))
        }

        val handlingNotificationInFetching = launchedWithNotificationHandler
        launchedWithNotificationHandler = false

        CoroutineScope(Dispatchers.IO).launch {

            val poisList = PointsOfInterest.getPointsOfInterest(forceSync = true)
            val leList = LiveEvents.getLiveEvents(forceSync = true)

            val mainFragment = buildMainFragment(poisList, leList,handlingNotificationInFetching)
            CoroutineScope(Dispatchers.Main).launch {
                supportFragmentManager.beginTransaction().apply {
                    replace(R.id.main_fragment, mainFragment)
                    setReorderingAllowed(true)
                    commit()
                }
            }

            poisList.forEach { poi -> backgroundService?.addGeofence(poi.markId, poi.latitude, poi.longitude) }
        }
    }

    /**
     * Creates an instance of [MainFragment] taking in consideration the value of
     * [launchedWithNotificationHandler] and **intent.action**.
     * Initializes [onLocationUpdated] and then sets [launchedWithNotificationHandler] to `false`.
     * @return [MainFragment] instance
     */
    private fun buildMainFragment(poisList: List<PointOfInterest>, leList: List<LiveEvent>,handlingNotificationInFetching: Boolean): MainFragment {
        val mainFragment = if(handlingNotificationInFetching) {
            when (intent.action) {
                getString(R.string.activity_place_place_recommendation) -> {
                    Log.i(TAG, "Handling a notification with a Point of Interest recommendation.")
                    val poi: PointOfInterest = intent.getParcelableExtra(getString(R.string.extra_point_of_interest))!!
                    MainFragment.newInstance(poisList, leList, poi, getString(R.string.activity_place_place_recommendation))
                }
                getString(R.string.activity_place_validity_recommendation) -> {
                    Log.i(TAG, "Handling a notification with a validity Point of Interest recommendation.")
                    val poi: PointOfInterest = intent.getParcelableExtra(getString(R.string.extra_point_of_interest))!!
                    MainFragment.newInstance(poisList, leList, poi, getString(R.string.activity_place_validity_recommendation))
                }
                getString(R.string.activity_new_live_event) -> {
                    Log.i(TAG, "Handling a notification with a new Live Event creation.")
                    val live: LiveEvent = intent.getParcelableExtra(getString(R.string.extra_live_event))!!
                    MainFragment.newInstance(poisList, leList, live)
                }
                getString(R.string.activity_friend_request_accepted) -> {
                    Log.i(TAG, "Handling a notification with a friend request accepted.")
                    val friend = intent.getStringExtra(getString(R.string.extra_friend_username))!!
                    MainFragment.newInstance(poisList, leList, friend,false)
                }
                getString(R.string.activity_new_friend_request) -> {
                    Log.i(TAG, "Handling a notification with a new friend request.")
                    val friend = intent.getStringExtra(getString(R.string.extra_friend_username))!!
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
        onLocationStatusUpdated = mainFragment::onLocationStatusUpdated

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
                    startGeofencingService()
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
            startGeofencingService()
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
     * Starts an instance of [BackgroundService] and binds the current activity to it so [backgroundService]
     * can refer to it.
     */
    private fun startLocationService() {
        Log.v(TAG, "startLocationService")

        val activeServicesSharedPrefs = getSharedPreferences(getString(R.string.sharedpreferences_active_services), Context.MODE_PRIVATE)

        if(!activeServicesSharedPrefs.contains("locationService")) {
            // The default value is true since we want the LocationService to start at least the first time.
            activeServicesSharedPrefs.edit().putBoolean("locationService", true).apply()
        }

        if(!activeServicesSharedPrefs.getBoolean("locationService", true)) {
            return
        }

        val startIntent = Intent(this, BackgroundService::class.java).apply {
            action = getString(R.string.background_location_start)
        }
        startService(startIntent)

        val bindIntent = Intent(this, BackgroundService::class.java)
        bindService(bindIntent, connection, Context.BIND_AUTO_CREATE)

        Toast.makeText(this, R.string.location_service_started, Toast.LENGTH_SHORT).show()
    }

    /**
     * Retrieves the instance of [AlarmManager] for running [PlaceRecommendationReceiver].
     */
    @SuppressLint("UnspecifiedImmutableFlag")
    private fun startGeofencingService() {
        Log.v(TAG, "startAlarmService")

        val activeServicesSharedPrefs = getSharedPreferences(getString(R.string.sharedpreferences_active_services), Context.MODE_PRIVATE)

        if(!activeServicesSharedPrefs.contains("geofencingService")) {
            activeServicesSharedPrefs.edit().putBoolean("geofencingService", false).apply()
        }

        if(!activeServicesSharedPrefs.getBoolean("geofencingService", false)) {
            return
        }

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(this, PlaceRecommendationReceiver::class.java).apply {
            action = getString(R.string.recommendation_periodic_alarm)
        }

        val recommendationBroadcast = PendingIntent.getBroadcast(
            this,
            0,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        alarmManager.setRepeating(
            Alarm.alarmType(),
            Alarm.firstRunMillis(),
            Alarm.repeatingRunTimeWindow(),
            recommendationBroadcast
        )

        Toast.makeText(this, R.string.periodic_place_recommendation_service_started, Toast.LENGTH_SHORT).show()
    }

    /**
     * @see BackgroundService.LocationListener.onLocationChanged
     */
    override fun onLocationChanged(service: Service, location: Location) {
        if (this::onLocationUpdated.isInitialized) {
            onLocationUpdated(location)
        }
    }

    override fun onLocationStatusChanged(service: Service, newStatus: Boolean) {
        if(this::onLocationStatusUpdated.isInitialized) {
            onLocationStatusUpdated(newStatus)
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

            val poiId = PointsOfInterest.addPointOfInterest(AddPointOfInterest(addPointOfInterestPoi))
            backgroundService?.addGeofence(poiId, addPointOfInterestPoi.latitude, addPointOfInterestPoi.longitude)

            CoroutineScope(Dispatchers.Main).launch {
                dialog.dismiss()
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
