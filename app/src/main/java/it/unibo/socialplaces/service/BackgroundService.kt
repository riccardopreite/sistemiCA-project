package it.unibo.socialplaces.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.location.Location
import it.unibo.socialplaces.config.Location as LocationConfig
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import it.unibo.socialplaces.R
import it.unibo.socialplaces.receiver.GeofenceBroadcastReceiver

@SuppressLint("UnspecifiedImmutableFlag")
class BackgroundService: Service() {
    // Listener
    interface LocationListener {
        fun onLocationChanged(service: Service, location: Location)
        fun onLocationStatusChanged(service: Service, newStatus: Boolean)
    }

    private var locationListener: LocationListener? = null

    // Binder
    inner class LocationBinder: Binder() {
        fun getService(): BackgroundService = this@BackgroundService
    }

    private val binder = LocationBinder()

    // Location manager
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    // Geofence manager
    private lateinit var geofencingClient: GeofencingClient

    // Pending Intent to handle geofence triggers
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
        intent.action = getString(R.string.recommendation_geofence_enter)
        PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    // Notification channel
    private val channel = NotificationChannel(
        NOTIFICATION_CHANNEL_ID,
        CHANNEL_NAME,
        NotificationManager.IMPORTANCE_NONE
    ).apply {
        lightColor = Color.BLUE
        lockscreenVisibility = Notification.VISIBILITY_PRIVATE
    }

    companion object {
        private val TAG = BackgroundService::class.qualifiedName!!

        const val NOTIFICATION_ID = 2
        const val NOTIFICATION_CHANNEL_ID = "it.unibo.socialplaces.location"
        const val CHANNEL_NAME = "SocialPlaces: Background Service"
    }

    // App state
    /**
     * If `true` then the current service is running, otherwise it is `false`.
     */
    private var isRunning = false

    private val geofenceIds: MutableList<String> = emptyList<String>().toMutableList()

    /**
     * Last location that was set.
     */
    private lateinit var lastLocation: Location

    private val locationUpdateCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            lastLocation = locationResult.lastLocation
//            Log.d(TAG, "Current location: (${lastLocation.latitude}, ${lastLocation.longitude})")
            locationListener?.onLocationChanged(this@BackgroundService, lastLocation)
            // Storing the location inside Android's Shared Preferences in order to
            // access it in other background tasks.
            val sharedPrefLocation = getSharedPreferences(getString(R.string.sharedpreferences_location_updates), Context.MODE_PRIVATE)?: return
            with (sharedPrefLocation.edit()) {
                putFloat("latitude", lastLocation.latitude.toFloat())
                putFloat("longitude", lastLocation.longitude.toFloat())
                apply()
            }
        }
    }

    /**
     * Sets an instance to be called when the location updates.
     */
    fun setLocationListener(l: LocationListener) {
        locationListener = l
    }

    @SuppressLint("MissingPermission")
    private fun checkLocationSettings(): Task<LocationSettingsResponse> {
        Log.v(TAG, "checkLocationSettings")

        val client: SettingsClient = LocationServices.getSettingsClient(this)
        return client.checkLocationSettings(LocationConfig.createLocationSettingsRequest()).apply {
            addOnSuccessListener {
                Log.d(TAG, "Starting location updates.")
                fusedLocationProviderClient.requestLocationUpdates(
                    LocationConfig.createLocationRequest(),
                    locationUpdateCallback,
                    Looper.getMainLooper()
                )
                createNotificationChannel()
                isRunning = true
                locationListener?.onLocationStatusChanged(this@BackgroundService, true)
            }
            addOnFailureListener {
                Log.e(TAG, "$it\nCould not start the location service.")
                isRunning = false
                locationListener?.onLocationStatusChanged(this@BackgroundService, false)
            }
        }
    }

    /**
     * Starts the location updates.
     * Precondition: permission to use location services is given.
     */
    private fun startLocationUpdates() {
        Log.v(TAG, "startLocationUpdates")
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        geofencingClient = LocationServices.getGeofencingClient(this)

        checkLocationSettings()
    }

    /**
     * Stops the location updates.
     * Precondition: the use of location is active.
     */
    private fun stopLocationUpdates() {
        Log.v(TAG, "stopLocationUpdates")
        fusedLocationProviderClient.removeLocationUpdates(locationUpdateCallback)
        locationListener?.onLocationStatusChanged(this, false)
        stopForeground(true)
        stopSelf()
        isRunning = false
    }

    /**
     * Adds new geofences given the data from [geofences].
     * The geofences not available in [geofences] but still working will be canceled.
     * @param geofences a list of triples (geofenceId, latitude, longitude)
     */
    @SuppressLint("MissingPermission")
    fun updateGeofences(geofences: List<Triple<String, Double, Double>>) {
        Log.v(TAG, "updateGeofences")
        val geofencesSet = geofences.map { it.first }.toSet() // Ex. {0,1,2}
        val geofenceIdsSet = geofenceIds.toSet() // Ex. {1,2,3}
        val toRemoveIds = geofenceIdsSet.minus(geofencesSet) // {1,2,3} \ {0,1,2} = {3}
        val toAddIds = geofencesSet.minus(geofenceIdsSet) // {0,1,2} \ {1,2,3} = {0}
        /* Hence we remove [toRemoveSet] from [geofenceIds] and later add [toAddSet]. */
        geofenceIds.removeAll(toRemoveIds)

        // Removing the no more available geofences and then adding the new ones.
        geofencingClient.removeGeofences(toRemoveIds.toList()).addOnSuccessListener {
            Log.i(TAG, "Removed all the geofences that were not available anymore ($toRemoveIds).")
        }.addOnFailureListener {
            if(toRemoveIds.toList().isEmpty()) {
                Log.i(TAG, "No geofences were removed, all available geofences are kept.")
            } else {
                Log.e(TAG, "Could not remove geofences with ids=$toRemoveIds.\n$it")
            }
        }.addOnCompleteListener {
            Log.i(TAG, "Adding the new geofences ($toAddIds)")
            if(toAddIds.isEmpty()){
                return@addOnCompleteListener
            }
            geofenceIds.addAll(toAddIds)
            val geofencesObjects = geofences.filter { toAddIds.contains(it.first) }.map {
                Geofence.Builder()
                    .setRequestId(it.first)
                    .setCircularRegion(it.second, it.third, resources.getInteger(R.integer.GEOFENCE_RADIUS_IN_METERS).toFloat())
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                    .build()
            }

            val geofencingRequest = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofences(geofencesObjects)
                .build()

            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
                addOnSuccessListener {
                    Log.v(TAG, "Added geofences with ids=${geofencingRequest.geofences.map{ it.requestId }}.")
                }
                addOnFailureListener {
                    it.message?.let { exc -> Log.e(TAG, exc) }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun addGeofence(geofenceId: String, latitude: Double, longitude: Double) {
        Log.v(TAG, "addGeofence")
        if(geofenceIds.contains(geofenceId)) {
            return
        }
        geofenceIds.add(geofenceId)

        val geofence = Geofence.Builder()
            .setRequestId(geofenceId)
            .setCircularRegion(
                latitude,
                longitude,
                resources.getInteger(R.integer.GEOFENCE_RADIUS_IN_METERS).toFloat()
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
            addOnSuccessListener {
                Log.v(TAG, "Added geofence with id: ${geofencingRequest.geofences[0].requestId}.")
            }
            addOnFailureListener {
                it.message?.let { exc -> Log.e(TAG, exc) }
            }
        }
    }

    /**
     * Returns whether the service is running or not.
     */
    fun isServiceRunning(): Boolean = isRunning

    /**
     * Creates a notification to be always displayed when this [BackgroundService] is active.
     */
    private fun createOngoingForegroundNotification(pendingIntent: PendingIntent): Notification {
        Log.v(TAG, "createOngoingForegroundNotification")
        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        return notificationBuilder
            .setOngoing(true)
            .setContentTitle(CHANNEL_NAME)
            .setContentText("Foreground location service is active.")
            .setContentIntent(pendingIntent)
            .setPriority(NotificationManager.IMPORTANCE_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    /**
     * Creates the notification channel and publishes the notification created by [createOngoingForegroundNotification].
     */
    private fun createNotificationChannel() {
        Log.v(TAG, "createNotificationChannel")

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = createOngoingForegroundNotification(pendingIntent)

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
           when (action) {
               getString(R.string.background_location_start) -> startLocationUpdates()
               getString(R.string.background_location_stop) -> stopLocationUpdates()
               else -> Unit
           }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        Log.v(TAG, "onDestroy")
        super.onDestroy()
        stopLocationUpdates()
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.v(TAG, "onBind")
        return binder
    }
}