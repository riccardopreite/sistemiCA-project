package com.example.maptry.activity

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.maptry.R
import com.example.maptry.domain.Friends
import com.example.maptry.domain.LiveEvents
import com.example.maptry.domain.PointsOfInterest
import com.example.maptry.fragment.FriendsFragment
import com.example.maptry.fragment.LiveEventsFragment
import com.example.maptry.fragment.PointsOfInterestFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ListActivity: AppCompatActivity(R.layout.activity_list) {
    companion object {
        val TAG: String = ListActivity::class.qualifiedName!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        intent.extras?.let { extra ->
            when (extra.get("screen")){
                R.id.friends_list -> {
                    CoroutineScope(Dispatchers.IO).launch {
                        val friendList = Friends.getFriends()
                        val listFragment = FriendsFragment.newInstance(friendList)
                        pushFragment(listFragment)
                    }
                }
                R.id.pois_list -> {
                    CoroutineScope(Dispatchers.IO).launch {
                        val poisList = PointsOfInterest.getPointsOfInterest()
                        val poisFragment = PointsOfInterestFragment.newInstance(poisList)
                        pushFragment(poisFragment)
                    }
                }
                R.id.lives_list -> {
                    CoroutineScope(Dispatchers.IO).launch {
                        val liveEventsList = LiveEvents.getLiveEvents()
                        val liveEventsFragment = LiveEventsFragment.newInstance(liveEventsList)
                        pushFragment(liveEventsFragment)
                    }
                }
                else -> {}
            }
        }
    }

    override fun onBackPressed() {
        if(supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            finish()
        }
    }

    private fun pushFragment(fragment: Fragment) {
        CoroutineScope(Dispatchers.Main).launch {
            supportFragmentManager.beginTransaction().apply {
                replace(R.id.list_fragment, fragment)
                setReorderingAllowed(true)
                // Setting null in the back stack implicitly disables the backbutton in the fragment.
//                addToBackStack(null)
                commit()
            }
        }
    }
}