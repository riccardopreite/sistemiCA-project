package com.example.maptry.activity

import android.Manifest
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
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
import com.example.maptry.R
import com.example.maptry.domain.LiveEvents
import com.example.maptry.domain.PointsOfInterest
import com.example.maptry.fragment.MainFragment
import com.example.maptry.fragment.dialog.CreatePoiOrLiveDialogFragment
import com.example.maptry.fragment.dialog.LiveEventDetailsDialogFragment
import com.example.maptry.fragment.dialog.PoiDetailsDialogFragment
import com.example.maptry.model.liveevents.AddLiveEvent
import com.example.maptry.model.liveevents.LiveEvent
import com.example.maptry.model.pointofinterests.AddPointOfInterest
import com.example.maptry.model.pointofinterests.AddPointOfInterestPoi
import com.example.maptry.model.pointofinterests.PointOfInterest
import com.example.maptry.service.LocationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity: AppCompatActivity(R.layout.activity_main),
    LocationService.LocationListener,
    CreatePoiOrLiveDialogFragment.CreatePoiDialogListener,
    PoiDetailsDialogFragment.PoiDetailsDialogListener,
    LiveEventDetailsDialogFragment.LiveEventDetailsDialogListener {
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
            Log.i(TAG, "Permission to use location GRANTED.")
        } else {
            Log.i(TAG, "Permission to use location DENIED.")
        }
    }

    /**
     * Permission request launcher (for background location permission).
     */
    private val requestBackgroundLocationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            isGranted: Boolean ->
        if(isGranted) {
            Log.i(TAG, "Permission to use (background) location GRANTED.")
        } else {
            Log.i(TAG, "Permission to use (background) location DENIED.")
        }
    }

    companion object {
        val TAG: String = MainActivity::class.qualifiedName!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        checkLocationPermissions()
    }

    private fun loadPoisAndLiveEvents(force: Boolean = false) {
        Log.v(TAG, "loadPoisAndLiveEvents")
        CoroutineScope(Dispatchers.IO).launch {
            val poisList = PointsOfInterest.getPointsOfInterest(forceSync = force)
            val leList = LiveEvents.getLiveEvents(forceSync = force)
            val mainFragment = MainFragment.newInstance(poisList, leList)
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
     * This method checks permissions to ACCESS_FINE_LOCATION and later calls [checkBackgroundLocationPermissions].
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
                checkBackgroundLocationPermissions()
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
                startLocationService()
                loadPoisAndLiveEvents(force = true)
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
        // Since background location is checked later, this check must be done earlier.
        when(permissions[0]) {
             Manifest.permission.ACCESS_BACKGROUND_LOCATION -> {
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationService()
                }
                loadPoisAndLiveEvents(force = true)
            }
            Manifest.permission.ACCESS_FINE_LOCATION -> {
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkBackgroundLocationPermissions()
                } else {
                    Toast.makeText(this, R.string.location_permission_denied, Toast.LENGTH_SHORT).show()
                }
            }
            else -> Unit
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
        if(locationServiceIsBound) {
            unbindService(connection)
            locationServiceIsBound = false
            stopLocationService()
        }
    }

    override fun onDestroy() {
        Log.v(TAG, "onDestroy")
        super.onDestroy()
    }

    override fun onLocationChanged(service: Service, location: Location) {
        Log.d(TAG, "Location reached MainActivity: $location")
    }

    override fun onAddLiveEvent(dialog: DialogFragment, addLiveEvent: AddLiveEvent) {
        Log.v(TAG, "CreatePoiOrLiveDialogFragment.onAddLiveEvent")
        CoroutineScope(Dispatchers.IO).launch {
            LiveEvents.addLiveEvent(addLiveEvent)

            CoroutineScope(Dispatchers.Main).launch {
                dialog.dismiss()
            }
        }
    }

    override fun onAddPointOfInterest(
        dialog: DialogFragment,
        addPointOfInterestPoi: AddPointOfInterestPoi
    ) {
        Log.v(TAG, "CreatePoiOrLiveDialogFragment.onAddPointOfInterest")
        CoroutineScope(Dispatchers.IO).launch {
            PointsOfInterest.addPointOfInterest(AddPointOfInterest(addPointOfInterestPoi))

            CoroutineScope(Dispatchers.Main).launch {
                dialog.dismiss()
            }
        }
    }

    override fun onShareButtonPressed(dialog: DialogFragment, poi: PointOfInterest) {
        Log.v(TAG, "PoiDetailsDialogFragment.onShareButtonPressed")
        sharePlace(poi.name, poi.address, poi.latitude, poi.longitude)
    }

    override fun onRouteButtonPressed(dialog: DialogFragment, address: String) {
        Log.v(TAG, "PoiDetailsDialogFragment.onRouteButtonPressed")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$address"))
        dialog.dismiss()
        startActivity(intent)
    }

    override fun onShareButtonPressed(dialog: DialogFragment, liveEvent: LiveEvent) {
        Log.v(TAG, "LiveEventDetailsDialogFragment.onShareButtonPressed")
        sharePlace(liveEvent.name, liveEvent.address, liveEvent.latitude, liveEvent.longitude)
    }

    private fun sharePlace(name: String, address: String, latitude: Double, longitude: Double) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, getString(R.string.share_place, name, address, latitude, longitude))
        }
        val createdIntent = Intent.createChooser(shareIntent,getString(R.string.share_place_intent, name))
        ContextCompat.startActivity(this, createdIntent, null)
    }
}
