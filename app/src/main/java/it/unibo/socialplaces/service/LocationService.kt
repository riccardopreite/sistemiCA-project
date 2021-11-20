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

class LocationService: Service() {
    // Listener
    interface LocationListener {
        fun onLocationChanged(service: Service, location: Location)
    }

    private var listener: LocationListener? = null

    // Binder
    inner class LocationBinder: Binder() {
        fun getService(): LocationService = this@LocationService
    }

    private val binder = LocationBinder()

    // Location manager
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    // Notification
    private val channel = NotificationChannel(
        NOTIFICATION_CHANNEL_ID,
        CHANNEL_NAME,
        NotificationManager.IMPORTANCE_NONE
    ).apply {
        lightColor = Color.BLUE
        lockscreenVisibility = Notification.VISIBILITY_PRIVATE
    }

    companion object {
        private val TAG = LocationService::class.qualifiedName

        const val START_LOCATION_SERVICE = "startLocationService"
        const val STOP_LOCATION_SERVICE = "stopLocationService"

        const val NOTIFICATION_ID = 2
        const val NOTIFICATION_CHANNEL_ID = "it.unibo.location"
        const val CHANNEL_NAME = "SocialPlaces: Background Location Retrieval Service"
    }

    // App state
    /**
     * If `true` then the current service is running, otherwise it is `false`.
     */
    private var isRunning = false

    /**
     * Last location that was set.
     */
    private lateinit var lastLocation: Location

    private val locationUpdateCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            lastLocation = locationResult.lastLocation
//            Log.d(TAG, "Current location: (${lastLocation.latitude}, ${lastLocation.longitude})")
            listener?.onLocationChanged(this@LocationService, lastLocation)
            // Storing the location inside Android's Shared Preferences in order to
            // access it in other background tasks.
            val sharedPrefLocation = getSharedPreferences(getString(R.string.location_preference), Context.MODE_PRIVATE)?: return
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
    fun setListener(l: LocationListener) {
        listener = l
    }

    @SuppressLint("MissingPermission")
    private fun checkSettings(): Task<LocationSettingsResponse> {
        Log.v(TAG, "settings")
        val client: SettingsClient = LocationServices.getSettingsClient(this)
        return client.checkLocationSettings(LocationConfig.createLocationSettingsRequest()).apply {
            addOnSuccessListener {
                fusedLocationProviderClient.requestLocationUpdates(
                    LocationConfig.createLocationRequest(),
                    locationUpdateCallback,
                    Looper.getMainLooper()
                )
                createNotificationChannel()
                isRunning = true
            }
            addOnFailureListener {
                Log.v(TAG, "$it\nCould not start the location service.")
                isRunning = false
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

        checkSettings()
    }

    /**
     * Stops the location updates.
     * Precondition: the use of location is active.
     */
    private fun stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationUpdateCallback)
        stopForeground(true)
        stopSelf()
        isRunning = false
    }

    /**
     * Returns whether the service is running or not.
     */
    fun isServiceRunning(): Boolean = isRunning

    /**
     * Creates a notification to be always displayed when this [LocationService] is active.
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
        intent?.let {
           it.action?.let { action ->
               when (action) {
                   START_LOCATION_SERVICE -> startLocationUpdates()
                   STOP_LOCATION_SERVICE -> stopLocationUpdates()
                   else -> Unit
               }
           }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
}