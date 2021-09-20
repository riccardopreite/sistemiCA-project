package com.example.maptry.activity

import android.Manifest
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.maptry.R
import com.example.maptry.service.LocationService

class MainActivity: AppCompatActivity(),
    LocationService.LocationListener {
    /**
     * Flag to memorize whether the user requested to utilize location services (foreground) or not.
     */
    private var locationPermissionAllowed: Boolean? = null
    /**
     * Flag to memorize whether the user requested to utilize location services (background) or not.
     */
    private var backgroundLocationPermissionAllowed: Boolean? = null

    private lateinit var locationService: LocationService
    private var locationServiceIsBound: Boolean = false
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as LocationService.LocationBinder
            locationService = binder.getService()
            locationService.setListener(this@MainActivity)
            locationServiceIsBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            locationServiceIsBound = false
        }
    }


    /**
     * Permission request launcher (for location permission).
     */
    private val requestLocationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            isGranted: Boolean ->
        if(isGranted) {
            locationPermissionAllowed = true
            Log.i(TAG, "Permission to use (background) location GRANTED.")
        } else {
            locationPermissionAllowed = false
            Log.i(TAG, "Permission to use (background) location DENIED.")
        }
    }

    /**
     * Permission request launcher (for background location permission).
     */
    private val requestBackgroundLocationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            isGranted: Boolean ->
        if(isGranted) {
            backgroundLocationPermissionAllowed = true
            Log.i(TAG, "Permission to use (background) location GRANTED.")
        } else {
            backgroundLocationPermissionAllowed = false
            Log.i(TAG, "Permission to use (background) location DENIED.")
        }
    }

    companion object {
        val TAG: String = MainActivity::class.qualifiedName!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkLocationPermissions()
        checkBackgroundLocationPermissions()
        if(locationPermissionAllowed != null &&
            backgroundLocationPermissionAllowed != null &&
            locationPermissionAllowed!! &&
            backgroundLocationPermissionAllowed!!) {
            startLocationService()
        }
    }

    /**
     * This method checks permissions to ACCESS_FINE_LOCATION and if enabled:
     * - enables the retrieval of location updates
     */
    private fun checkLocationPermissions() {
        Log.v(TAG, "checkLocationPermissions")
        // Location permission check
        when {
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                Log.i(TAG, "Enabling location services since permissions were given.")
                locationPermissionAllowed = true
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
     * This method checks permissions to ACCESS_BACKGROUND_LOCATION and if enabled:
     * - enables the retrieval of background location updates
     */
    private fun checkBackgroundLocationPermissions() {
        Log.v(TAG, "checkBackgroundLocationPermissions")
        // Background location permission check
        when {
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                Log.i(TAG, "Enabling background location services since permissions were given.")
                locationPermissionAllowed = true
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
        // Login independently of the result of the permission request (of course the app functions are limited)
        if(locationPermissionAllowed != null && backgroundLocationPermissionAllowed != null) {
            if(locationPermissionAllowed!! && backgroundLocationPermissionAllowed!!) {
                startLocationService()
            } else {
                Toast.makeText(this, R.string.location_permission_denied, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startLocationService() {
        Log.v(TAG, "startLocationService")
        val startIntent = Intent(applicationContext, LocationService::class.java)
        startIntent.action = LocationService.START_LOCATION_SERVICE
        startService(startIntent)
        val bindIntent = Intent(this, LocationService::class.java)
        bindService(bindIntent, connection, Context.BIND_AUTO_CREATE)
        Toast.makeText(this, R.string.location_service_started, Toast.LENGTH_SHORT).show()
    }

    private fun stopLocationService() {
        Log.v(TAG, "stopLocationService")
        val stopIntent = Intent(applicationContext, LocationService::class.java)
        stopIntent.action = LocationService.STOP_LOCATION_SERVICE
        startService(stopIntent)
        Toast.makeText(this, R.string.location_service_stopped, Toast.LENGTH_SHORT).show()
    }

    override fun onStop() {
        Log.v(TAG, "onStop")
        super.onStop()
        unbindService(connection)
        locationServiceIsBound = false
        stopLocationService()
    }

    override fun onDestroy() {
        Log.v(TAG, "onDestroy")
        super.onDestroy()
    }

    override fun onLocationChanged(service: Service, location: Location) {
        Log.d(TAG, "Location reached MainActivity: $location")
    }
}
