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
import it.unibo.socialplaces.R
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
import it.unibo.socialplaces.service.LocationService
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
     * Permission request launcher (for read activity permission).
     */
    private val requestHumanActivityPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            isGranted: Boolean ->
        if(isGranted) {
            Log.i(TAG, "Permission to read human activity GRANTED.")
        } else {
            Log.i(TAG, "Permission to read human activity DENIED.")
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

    /**
     * Callback for updating the map.
     */
    lateinit var onLocationUpdated: (Location) -> Unit

    companion object {
        val TAG: String = MainActivity::class.qualifiedName!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        checkActivityPermission()
    }

    private fun loadPoisAndLiveEvents(force: Boolean = false) {
        Log.v(TAG, "loadPoisAndLiveEvents")
        CoroutineScope(Dispatchers.IO).launch {
            val poisList = PointsOfInterest.getPointsOfInterest(forceSync = force)
            val leList = LiveEvents.getLiveEvents(forceSync = force)
            val mainFragment = MainFragment.newInstance(poisList, leList)
            onLocationUpdated = mainFragment::onCurrentLocationUpdated
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
     * This method checks permissions to ACTIVITY_RECOGNITION.
     */
    private fun checkActivityPermission() {
        Log.v(TAG, "checkActivityPermission")
        // Location permission check
        when {
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED -> {
                Log.i(TAG, "Enabling activity services since permissions were given.")
                checkLocationPermissions()
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
                startAlarmService()
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
            Manifest.permission.ACTIVITY_RECOGNITION -> {
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkLocationPermissions()
                }
            }
            Manifest.permission.ACCESS_FINE_LOCATION -> {
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkBackgroundLocationPermissions()
                } else {
                    Toast.makeText(this, R.string.location_permission_denied, Toast.LENGTH_SHORT).show()
                }
            }
            Manifest.permission.ACCESS_BACKGROUND_LOCATION -> {
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationService()
                    startAlarmService()
                }
                loadPoisAndLiveEvents(force = true)
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

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun startAlarmService(){
        Log.v(TAG, "startAlarmService")
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(this, RecommendationAlarm::class.java)

        val recommendationIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, 0)

//        try {
//            Log.d(TAG, "Stopping alarm")
//            alarmManager.cancel(recommendationIntent)
//        } catch (e:Exception) {
//            Log.d(TAG, "Alarm never started")
//        }
//        Log.d(TAG, "Starting Alarm")
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis()+10,
            AlarmManager.INTERVAL_HOUR * 3,
            recommendationIntent
        )
    }

    private fun stopLocationService() {
        Log.v(TAG, "stopLocationService")
        val stopIntent = Intent(applicationContext, LocationService::class.java)
        stopIntent.action = LocationService.STOP_LOCATION_SERVICE
        startService(stopIntent)
        Toast.makeText(this, R.string.location_service_stopped, Toast.LENGTH_LONG).show()
    }

    override fun onStop() {
        Log.v(TAG, "onStop")
        super.onStop()
        // TODO Add possibility to disable it.
//        if(locationServiceIsBound) {
//            unbindService(connection)
//            locationServiceIsBound = false
//            stopLocationService()
//        }
    }

    override fun onDestroy() {
        Log.v(TAG, "onDestroy")
        super.onDestroy()
    }

    override fun onLocationChanged(service: Service, location: Location) {
        Log.d(TAG, "Location reached MainActivity: $location")
        if(this::onLocationUpdated.isInitialized) {
            onLocationUpdated(location)
        }
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
        Log.v(TAG, "Poi&LiveDetailsDialogFragment.onRouteButtonPressed")
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
