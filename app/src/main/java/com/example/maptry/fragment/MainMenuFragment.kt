package com.example.maptry.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.example.maptry.R
import com.example.maptry.activity.list.FriendsListActivity
import com.example.maptry.activity.list.LiveEventsListActivity
import com.example.maptry.activity.list.PointsOfInterestListActivity
import com.example.maptry.config.Auth
import com.example.maptry.databinding.FragmentMainMenuBinding
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

        // TODO sono necessari questi View.VISIBLE?
        user.visibility = View.VISIBLE
        email.visibility = View.VISIBLE
        icon.visibility = View.VISIBLE
        close.visibility = View.VISIBLE

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