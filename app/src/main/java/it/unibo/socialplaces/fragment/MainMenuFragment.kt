package it.unibo.socialplaces.fragment

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import it.unibo.socialplaces.R
import it.unibo.socialplaces.activity.list.FriendsListActivity
import it.unibo.socialplaces.activity.list.LiveEventsListActivity
import it.unibo.socialplaces.activity.list.PointsOfInterestListActivity
import it.unibo.socialplaces.config.Auth
import it.unibo.socialplaces.databinding.FragmentMainMenuBinding
import com.google.android.material.navigation.NavigationView
import com.squareup.picasso.Picasso
import it.unibo.socialplaces.config.Alarm
import it.unibo.socialplaces.receiver.PeriodicPlaceRecommendationReceiver
import it.unibo.socialplaces.service.BackgroundService

class MainMenuFragment : Fragment(R.layout.fragment_main_menu),
    NavigationView.OnNavigationItemSelectedListener {
    // UI
    private var _binding: FragmentMainMenuBinding? = null
    private val binding get() = _binding!!

    // App state
    private var backgroundService: BackgroundService? = null
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            Log.d(TAG, "BackgroundService connected to MainMenuFragment.")
            val binder = service as BackgroundService.LocationBinder
            backgroundService = binder.getService()

            val activeServicesSharedPrefs = requireContext().getSharedPreferences(getString(R.string.sharedpreferences_active_services), Context.MODE_PRIVATE)

            val locationServiceSwitch = binding.menuNavView.menu.findItem(R.id.location_service_switch).actionView as SwitchCompat
            locationServiceSwitch.isChecked = activeServicesSharedPrefs.getBoolean("locationService", true)


            val geofencingServiceSwitch = binding.menuNavView.menu.findItem(R.id.geofencing_service_switch).actionView as SwitchCompat
            geofencingServiceSwitch.isChecked = activeServicesSharedPrefs.getBoolean("geofencingService", false)

            isServiceBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            Log.d(TAG, "BackgroundService disconnected from MainMenuFragment.")
        }
    }
    private var isServiceBound: Boolean = false

    companion object {
        private val TAG: String = MainMenuFragment::class.qualifiedName!!

        private const val ARG_ISSERVICEBOUND = "isServiceBound"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.v(TAG, "onViewCreated")
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentMainMenuBinding.bind(view)

        binding.menuNavView.setNavigationItemSelectedListener(this)

        /**
         * Launching the service bounding here so [connection] has access to [binding].
         */
        bindLocationService()

        val navBar = binding.menuNavView.getHeaderView(0)

        val icon = navBar.findViewById<ImageView>(R.id.imageView)
        val user = navBar.findViewById<TextView>(R.id.user)
        val email = navBar.findViewById<TextView>(R.id.email)
        val close = navBar.findViewById<ImageView>(R.id.close)
        val locationServiceSwitch = binding.menuNavView.menu.findItem(R.id.location_service_switch).actionView as SwitchCompat
        val geofencingServiceSwitch = binding.menuNavView.menu.findItem(R.id.geofencing_service_switch).actionView as SwitchCompat

        Auth.getUserProfileIcon()?.let { Picasso.get().load(it).into(icon) }
        Auth.getUserFullName()?.let { user.text = it }
        Auth.getUserEmailAddress()?.let { email.text = it }

        close.setOnClickListener {
            activity?.supportFragmentManager?.popBackStack()
        }

        val activeServicesSharedPrefs = requireContext().getSharedPreferences(getString(R.string.sharedpreferences_active_services), Context.MODE_PRIVATE)

        locationServiceSwitch.setOnClickListener {
            val isLocationServiceActive = activeServicesSharedPrefs.getBoolean("locationService", true)
            val isGeofencingServiceActive = activeServicesSharedPrefs.getBoolean("geofencingService", false)
            if(isLocationServiceActive) {
                stopLocationService()
                if(isGeofencingServiceActive) {
                    stopPeriodicPlaceRecommendation()
                    geofencingServiceSwitch.isChecked = false
                    activeServicesSharedPrefs.edit().putBoolean("geofencingService", geofencingServiceSwitch.isChecked).apply()
                }
            } else {
                startLocationService()
            }
            locationServiceSwitch.isChecked = !isLocationServiceActive
            activeServicesSharedPrefs.edit().putBoolean("locationService", locationServiceSwitch.isChecked).apply()
        }

        geofencingServiceSwitch.setOnClickListener {
            val isLocationServiceActive = activeServicesSharedPrefs.getBoolean("locationService", true)
            if(!isLocationServiceActive) {
                geofencingServiceSwitch.isChecked = false
                return@setOnClickListener
            }
            val isGeofencingServiceActive = activeServicesSharedPrefs.getBoolean("geofencingService", false)
            if(isGeofencingServiceActive) {
                stopPeriodicPlaceRecommendation()
            } else {
                startPeriodicPlaceRecommendation()
            }
            geofencingServiceSwitch.isChecked = !isGeofencingServiceActive
            activeServicesSharedPrefs.edit().putBoolean("geofencingService", geofencingServiceSwitch.isChecked).apply()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        Log.v(TAG, "onNavigationItemSelected")
        val menuIntent = when(item.itemId) {
            R.id.friends_list -> Intent(context, FriendsListActivity::class.java)
            R.id.pois_list -> Intent(context, PointsOfInterestListActivity::class.java)
            R.id.lives_list -> Intent(context, LiveEventsListActivity::class.java)
            else -> null
        }
        return menuIntent?.let {
            startActivity(it)
            true
        } ?: run {
            false
        }
    }

    override fun onDestroyView() {
        Log.v(TAG, "onDestroyView")
        super.onDestroyView()
        _binding = null
        unbindLocationService()
    }

    /**
     * Enables the binding between this fragment and the [BackgroundService].
     */
    private fun bindLocationService() {
        val activeServicesSharedPrefs = requireContext().getSharedPreferences(getString(R.string.sharedpreferences_active_services), Context.MODE_PRIVATE)
        val isLocationServiceActive = activeServicesSharedPrefs.getBoolean("locationService", true)
        if(isLocationServiceActive) {
            val bindIntent = Intent(requireContext(), BackgroundService::class.java)
            requireContext().bindService(bindIntent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    /**
     * Disables the binding between this fragment and the [BackgroundService].
     */
    private fun unbindLocationService() {
        Log.v(TAG, "unbindLocationService")
        if(isServiceBound) {
            requireContext().unbindService(connection)
            backgroundService = null
            isServiceBound = false
        }
    }

    /**
     * Starts the location service [BackgroundService] and binds this fragment to it (therefore initializes
     * [backgroundService]).
     */
    private fun startLocationService() {
        Log.v(TAG, "startLocationService")
        val startIntent = Intent(requireContext(), BackgroundService::class.java).apply {
            action = getString(R.string.background_location_start)
        }
        requireContext().startService(startIntent)
        bindLocationService()
        Toast.makeText(requireContext(), R.string.location_service_started, Toast.LENGTH_SHORT).show()
    }

    /**
     * Stops the location service [BackgroundService] and unbinds this fragment from it (therefore initializes
     * [backgroundService]).
     */
    private fun stopLocationService() {
        Log.v(TAG, "stopLocationService")
        val stopIntent = Intent(requireContext(), BackgroundService::class.java).apply {
            action = getString(R.string.background_location_stop)
        }
        // startService is correct because of the implementation of BackgroundService.onStartCommand()
        requireContext().startService(stopIntent)
        unbindLocationService()
        Toast.makeText(requireContext(), R.string.location_service_stopped, Toast.LENGTH_SHORT).show()
    }

    /**
     * Starts the geofencing broadcast receiver [PeriodicPlaceRecommendationReceiver] that starts the human activity recognition.
     */
    @SuppressLint("UnspecifiedImmutableFlag")
    private fun startPeriodicPlaceRecommendation() {
        Log.v(TAG, "startPeriodicPlaceRecommendation")
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(requireContext(), PeriodicPlaceRecommendationReceiver::class.java).apply {
            action = getString(R.string.recommendation_periodic_alarm)
        }
        val recommendationBroadcast = PendingIntent.getBroadcast(
            requireContext(), 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT
        )
        alarmManager.setRepeating(Alarm.alarmType(), Alarm.firstRunMillis(), Alarm.repeatingRunTimeWindow(), recommendationBroadcast)
        Toast.makeText(requireContext(), R.string.periodic_place_recommendation_service_started, Toast.LENGTH_SHORT).show()
    }

    /**
     * Stops the geofencing broadcast receiver [PeriodicPlaceRecommendationReceiver] that stops also the human activity recognition.
     */
    @SuppressLint("UnspecifiedImmutableFlag")
    private fun stopPeriodicPlaceRecommendation() {
        Log.v(TAG, "stopPeriodicPlaceRecommendation")
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(requireContext(), PeriodicPlaceRecommendationReceiver::class.java).apply {
            action = getString(R.string.recommendation_periodic_alarm)
        }
        val recommendationBroadcast = PendingIntent.getBroadcast(
            requireContext(), 0, alarmIntent, PendingIntent.FLAG_NO_CREATE
        )
        alarmManager.cancel(recommendationBroadcast)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(ARG_ISSERVICEBOUND, isServiceBound)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        isServiceBound = savedInstanceState?.getBoolean(ARG_ISSERVICEBOUND, false) ?: false
    }
}