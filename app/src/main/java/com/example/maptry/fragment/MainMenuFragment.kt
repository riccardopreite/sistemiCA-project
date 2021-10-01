package com.example.maptry.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import com.example.maptry.R
import com.example.maptry.activity.ListActivity
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
        binding.navView.setNavigationItemSelectedListener(this)
        val navBar = binding.navView.getHeaderView(0)

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
        val menuIntent = Intent(this.context,ListActivity::class.java)
        print("ITEM ID " + item.itemId)
        menuIntent.putExtra("screen",item.itemId)
        println("START LIST ACTIVITY")
        startActivity(menuIntent)
        return true
        /*return when(item.itemId) {
            R.id.list -> {

                return true
            }
            R.id.friend -> {
                return true
            }
            R.id.live -> {
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }*/
    }

    override fun onDestroyView() {
        Log.v(TAG, "onDestroyView")
        super.onDestroyView()
        _binding = null
    }
}