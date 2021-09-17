package com.example.maptry.activity

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.maptry.R
import com.example.maptry.service.LocationService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices

class MainActivity: AppCompatActivity() {
    /**
     * Flag to memorize whether the user requested to utilize location services or not.
     */
    private var locationPermissionAllowed: Boolean = false

    /**
     * Permission request launcher (for location permission).
     */
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            isGranted: Boolean ->
        if(isGranted) {
            locationPermissionAllowed = true
            Log.i(LoginActivity.TAG, "Permission to use location GRANTED")
        } else {
            locationPermissionAllowed = false
            Log.i(LoginActivity.TAG, "Permission to use location DENIED")
        }
    }

    companion object {
        val TAG: String = MainActivity::class.qualifiedName!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
    }

    /**
     * This method checks permissions to ACCESS_FINE_LOCATION and if enabled:
     * - enables the retrieval of location updates
     * - checks if the user is logged in via Google (authentication provided by the device) and if so, logs in via Firebase
     * - if the use is not logged in, it asks the user to log in
     * If
     */
    private fun checkPermissionsAndSignIn() {
        Log.v(TAG, "checkPermissionsAndSignIn")
        // Location permission check
        when {
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                Log.i(LoginActivity.TAG, "Enabling location services since permissions were given.")
                locationPermissionAllowed = true

                startLocationService()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                Log.i(LoginActivity.TAG, "Asking the user for permissions (rationale).")
                // addition rationale should be displayed
                val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                builder.setTitle(title)
                    .setMessage(R.string.location_services_required)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        requestPermissionLauncher.launch(
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    } // The user can only accept location functionalities.
                builder.create().show()
            }
            else -> {
                Log.i(LoginActivity.TAG, "Asking the user for permissions.")
                requestPermissionLauncher.launch(
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        Log.v(TAG, "onRequestPermissionsResult")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Login independently of the result of the permission request (of course the app functions are limited)
        if(locationPermissionAllowed) {
            startLocationService()
        } else {
            Toast.makeText(this, R.string.location_permission_denied, Toast.LENGTH_SHORT).show()
        }
    }

    private fun startLocationService() {
        val startIntent = Intent(applicationContext, LocationService::class.java)
        startIntent.action = LocationService.START_LOCATION_SERVICE
        startService(startIntent)
        Toast.makeText(this, R.string.location_service_started, Toast.LENGTH_SHORT).show()
    }

    private fun stopLocationService() {
        val stopIntent = Intent(applicationContext, LocationService::class.java)
        stopIntent.action = LocationService.STOP_LOCATION_SERVICE
        startService(stopIntent)
        Toast.makeText(this, R.string.location_service_stopped, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        Log.v(TAG, "onDestroy")
        super.onDestroy()
    }
}
