package com.example.maptry.activity

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.example.maptry.R
import com.example.maptry.domain.Friends
import com.example.maptry.domain.LiveEvents
import com.example.maptry.domain.PointsOfInterest
import com.example.maptry.fragment.FriendsFragment
import com.example.maptry.fragment.LiveEventsFragment
import com.example.maptry.fragment.PointsOfInterestFragment
import com.example.maptry.fragment.dialog.AddFriendDialogFragment
import com.example.maptry.model.friends.Friend
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ListActivity: AppCompatActivity(R.layout.activity_list),
    AddFriendDialogFragment.AddFriendDialogListener {
    companion object {
        val TAG: String = ListActivity::class.qualifiedName!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        intent.extras?.let { extra ->
            when (extra.get("screen")){
                R.id.friends_list -> {
                    // Using findViewById(android.R.id.content) is a workaround for accessing a view instance
                    val snackbar = Snackbar.make(findViewById(android.R.id.content), R.string.loading_friends, Snackbar.LENGTH_INDEFINITE)
                    snackbar.show()
                    CoroutineScope(Dispatchers.IO).launch {
                        val friendList = Friends.getFriends(forceSync = true)
                        val listFragment = FriendsFragment.newInstance(friendList)
                        pushFragment(listFragment)
                        CoroutineScope(Dispatchers.Main).launch { snackbar.dismiss() }
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

    private fun pushFragment(fragment: Fragment) {
        CoroutineScope(Dispatchers.Main).launch {
            supportFragmentManager.beginTransaction().apply {
                replace(R.id.list_fragment, fragment)
                setReorderingAllowed(true)
                commit()
            }
        }
    }

    override fun sendFriendshipRequest(dialog: DialogFragment, username: String) {
        CoroutineScope(Dispatchers.IO).launch {
            Friends.addFriend(username)

            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(this@ListActivity, R.string.friend_request_sent, Toast.LENGTH_SHORT).show()
            }
        }
    }
}