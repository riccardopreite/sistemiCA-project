package it.unibo.socialplaces.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import it.unibo.socialplaces.R
import it.unibo.socialplaces.activity.list.FriendsListActivity
import it.unibo.socialplaces.activity.list.LiveEventsListActivity
import it.unibo.socialplaces.activity.list.PointsOfInterestListActivity
import it.unibo.socialplaces.config.Auth
import it.unibo.socialplaces.databinding.FragmentMainMenuBinding
import com.google.android.material.navigation.NavigationView
import com.squareup.picasso.Picasso

class MainMenuFragment : Fragment(R.layout.fragment_main_menu),
    NavigationView.OnNavigationItemSelectedListener {
    // UI
    private var _binding: FragmentMainMenuBinding? = null
    private val binding get() = _binding!!

    companion object {
        private val TAG: String = MainMenuFragment::class.qualifiedName!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.v(TAG, "onViewCreated")
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMainMenuBinding.bind(view)
        binding.menuNavView.setNavigationItemSelectedListener(this)
        val navBar = binding.menuNavView.getHeaderView(0)

        val icon = navBar.findViewById<ImageView>(R.id.imageView)
        val user = navBar.findViewById<TextView>(R.id.user)
        val email = navBar.findViewById<TextView>(R.id.email)
        val close = navBar.findViewById<ImageView>(R.id.close)

        val userIconUri = Auth.getUserProfileIcon()

        // Loading the user icon inside ImageView icon.
        val picasso = Picasso.get()
        picasso.load(userIconUri).into(icon)
        user.text = Auth.getUserFullName()
        email.text = Auth.getUserEmailAddress()

        close.setOnClickListener {
            activity?.let {
                it.supportFragmentManager.popBackStack()
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
    }
}