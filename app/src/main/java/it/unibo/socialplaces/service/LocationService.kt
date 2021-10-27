package it.unibo.socialplaces.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task

class LocationService: Service() {
    interface LocationListener {
        fun onLocationChanged(service: Service, location: Location)
    }

    inner class LocationBinder: Binder() {
        fun getService(): LocationService = this@LocationService
    }
    
    companion object {
        private const val TAG = "LocationService"

        const val START_LOCATION_SERVICE = "startLocationService"
        const val STOP_LOCATION_SERVICE = "stopLocationService"

        const val NOTIFICATION_ID = 2
        const val NOTIFICATION_CHANNEL_ID = "it.unibo"
        const val CHANNEL_NAME = "Maptry: Background Location Retrieval Service"
    }

    private val binder = LocationBinder()

    private var listener: LocationListener? = null

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private val locationRequest: LocationRequest = LocationRequest.create().apply {
        interval = 5000
        fastestInterval = 2000
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY // Forse un po' troppo.
//        priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
    }

    private val locationUpdateCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            Log.v(TAG, "LocationCallback.onLocationResult $locationResult")
            lastLocation = locationResult.lastLocation
            Log.d(TAG, "Current location: (${lastLocation.latitude}, ${lastLocation.longitude})")
            listener?.onLocationChanged(this@LocationService, lastLocation)
        }
    }

    private val channel = NotificationChannel(
        NOTIFICATION_CHANNEL_ID,
        CHANNEL_NAME,
        NotificationManager.IMPORTANCE_NONE
    ).apply {
        lightColor = Color.BLUE
        lockscreenVisibility = Notification.VISIBILITY_PRIVATE
    }

    private lateinit var lastLocation: Location

    fun setListener(l: LocationListener) {
        listener = l
    }

    private fun createLocationSettingsRequest(): LocationSettingsRequest {
        Log.v(TAG, "createLocationSettingsRequest")
        return LocationSettingsRequest
            .Builder()
            .addLocationRequest(locationRequest)
            .build()
    }

    private fun checkSettings(): Task<LocationSettingsResponse> {
        Log.v(TAG, "settings")
        val client: SettingsClient = LocationServices.getSettingsClient(this)
        return client.checkLocationSettings(createLocationSettingsRequest())
    }

    /**
     * Starts the location updates.
     * Precondition: permission to use location services is given.
     */
    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        Log.v(TAG, "startLocationUpdates")
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        val settings = checkSettings()
        settings.apply {
            addOnSuccessListener {
                fusedLocationProviderClient.requestLocationUpdates(
                    locationRequest,
                    locationUpdateCallback,
                    Looper.getMainLooper()
                )

                createNotificationChannel()
            }
            addOnFailureListener {
                Log.v(TAG, "Could not start the location service because")
            }
        }


    }

    /**
     * Stops the location updates.
     * Precondition: the use of location is active.
     */
    private fun stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationUpdateCallback)
        stopForeground(true)
        stopSelf()
    }


    private fun createNotification(pendingIntent: PendingIntent): Notification {
        Log.v(TAG, "createNotification")
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

    private fun createNotificationChannel() {
        Log.v(TAG, "createNotificationChannel")
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
        val resultIntent = Intent()
        val pendingIntent = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_IMMUTABLE)
        val notification = createNotification(pendingIntent)
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