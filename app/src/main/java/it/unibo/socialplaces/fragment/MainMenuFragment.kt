package it.unibo.socialplaces.fragment

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
import it.unibo.socialplaces.activity.MainActivity
import it.unibo.socialplaces.service.LocationService

class MainMenuFragment : Fragment(R.layout.fragment_main_menu),
    NavigationView.OnNavigationItemSelectedListener {
    // UI
    private var _binding: FragmentMainMenuBinding? = null
    private val binding get() = _binding!!

    // App state
    private var locationService: LocationService? = null
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            Log.d(TAG, "LocationService connected to MainMenuFragment.")
            val binder = service as LocationService.LocationBinder
            locationService = binder.getService()

            val locationServiceSwitch = binding.menuNavView.menu.findItem(R.id.location_service_switch).actionView as SwitchCompat
            locationServiceSwitch.isChecked = locationService?.isServiceRunning() == true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            Log.d(TAG, "LocationService disconnected from MainMenuFragment.")
            locationService = null
        }
    }

    companion object {
        private val TAG: String = MainMenuFragment::class.qualifiedName!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.v(TAG, "onViewCreated")
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMainMenuBinding.bind(view)
        binding.menuNavView.setNavigationItemSelectedListener(this)

        /**
         * Launching the service bounding here so [connection] has access to [binding].
         */
        val bindIntent = Intent(requireContext(), LocationService::class.java)
        requireContext().bindService(bindIntent, connection, Context.BIND_AUTO_CREATE)

        val navBar = binding.menuNavView.getHeaderView(0)

        val icon = navBar.findViewById<ImageView>(R.id.imageView)
        val user = navBar.findViewById<TextView>(R.id.user)
        val email = navBar.findViewById<TextView>(R.id.email)
        val close = navBar.findViewById<ImageView>(R.id.close)
        val locationServiceSwitch = binding.menuNavView.menu.findItem(R.id.location_service_switch).actionView as SwitchCompat

        val userIconUri = Auth.getUserProfileIcon()
        if(userIconUri != null){
            // Loading the user icon inside ImageView icon.
            val picasso = Picasso.get()
            picasso.load(userIconUri).into(icon)
            user.text = Auth.getUserFullName()
            email.text = Auth.getUserEmailAddress()
        }

        close.setOnClickListener {
            activity?.supportFragmentManager?.popBackStack()
        }

        locationServiceSwitch.setOnClickListener {
            locationService?.let {
                if(it.isServiceRunning()) {
                    stopLocationService()
                    locationServiceSwitch.isChecked = false
                } else {
                    startLocationService()
                    locationServiceSwitch.isChecked = true
                }
            }
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
        requireContext().unbindService(connection)
    }

    private fun startLocationService() {
        Log.v(MainActivity.TAG, "startLocationService")
        val startIntent = Intent(requireContext(), LocationService::class.java).apply {
            action = LocationService.START_LOCATION_SERVICE
        }
        requireContext().startService(startIntent)
        val bindIntent = Intent(requireContext(), LocationService::class.java)
        requireContext().bindService(bindIntent, connection, Context.BIND_AUTO_CREATE)
        Toast.makeText(requireContext(), R.string.location_service_started, Toast.LENGTH_SHORT).show()
    }

    private fun stopLocationService() {
        Log.v(TAG, "stopLocationService")
        val stopIntent = Intent(requireContext(), LocationService::class.java).apply {
            action = LocationService.STOP_LOCATION_SERVICE
        }
        // startService is correct because of the implementation of LocationService.onStartCommand()
        requireContext().startService(stopIntent)
        Toast.makeText(requireContext(), R.string.location_service_stopped, Toast.LENGTH_LONG).show()
    }
}