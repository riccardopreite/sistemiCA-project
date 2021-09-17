package com.example.maptry.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task

class LocationService: Service() {
    companion object {
        private const val TAG = "LocationService"
        const val REQUEST_LOCATION_PERMISSIONS = 1
        const val REQUEST_CHECK_SETTINGS = 2

        const val START_LOCATION_SERVICE = "startLocationService"
        const val STOP_LOCATION_SERVICE = "stopLocationService"

        const val NOTIFICATION_ID = 2
        const val NOTIFICATION_CHANNEL_ID = "com.example"
        const val CHANNEL_NAME = "Maptry: Background Location Retrieval Service"
    }

    private lateinit var _fusedLocationProviderClient: FusedLocationProviderClient

    private val fusedLocationProviderClient get() = _fusedLocationProviderClient

    private val locationRequest: LocationRequest = LocationRequest.create().apply {
        interval = 20000
        fastestInterval = 10000
//            priority = LocationRequest.PRIORITY_HIGH_ACCURACY // Forse un po' troppo.
        priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
    }

    private val locationUpdateCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            Log.v(TAG, "LocationCallback.onLocationResult")
            _lastLocation = locationResult.lastLocation
            Log.d(TAG, "Current location: (${lastLocation.latitude}, ${lastLocation.longitude})")
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

    private lateinit var _lastLocation: Location

    val lastLocation get(): Location = _lastLocation

    private fun createLocationSettingsRequest(): LocationSettingsRequest {
        Log.v(TAG, "createLocationSettingsRequest")
        // TODO Check
        return LocationSettingsRequest
            .Builder()
            .addLocationRequest(locationRequest)
            .build()
    }

    fun settings(context: Context): Task<LocationSettingsResponse> {
        Log.v(TAG, "settings")
        val client: SettingsClient = LocationServices.getSettingsClient(context)
        return client.checkLocationSettings(createLocationSettingsRequest())
    }

    /**
     * Starts the location updates.
     * Precondition: permission to use location services is given.
     */
    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        _fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        Log.v(TAG, "startLocationUpdates")
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationUpdateCallback,
            Looper.getMainLooper()
        )

        createNotificationChannel()
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
        val notificationBuilder = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
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
        val pendingIntent = PendingIntent.getActivity(applicationContext, 0, resultIntent, PendingIntent.FLAG_IMMUTABLE)
        val notification = createNotification(pendingIntent)
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
       intent?.let {
           it.action?.let { action ->
                when(action) {
                    START_LOCATION_SERVICE -> startLocationUpdates()
                    STOP_LOCATION_SERVICE -> stopLocationUpdates()
                    else -> Unit
                }
           }
       }
        return super.onStartCommand(intent, flags, startId)
    }
    override fun onBind(intent: Intent?): IBinder? {
        throw UnsupportedOperationException("Not yet implemented.")
    }
}