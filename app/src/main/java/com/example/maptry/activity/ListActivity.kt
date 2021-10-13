package com.example.maptry.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
import com.example.maptry.fragment.dialog.*
import com.example.maptry.model.pointofinterests.AddPointOfInterest
import com.example.maptry.model.pointofinterests.AddPointOfInterestPoi
import com.example.maptry.model.pointofinterests.PointOfInterest
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ListActivity: AppCompatActivity(R.layout.activity_list),
    AddFriendDialogFragment.AddFriendDialogListener,
    EliminatePointOfInterestDialogFragment.EliminatePointOfInterestDialogListener,
    PointsOfInterestFragment.PointsOfInterestDialogListener,
    FriendDialogFragment.FriendDialogListener,
    FriendPoiDialogFragment.FriendPoiDialogListener {
    companion object {
        private val TAG: String = ListActivity::class.qualifiedName!!

        private const val ARG_POITODELETE = "poiToDelete"
    }

    // App state
    private var poiToDelete: PointOfInterest? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        savedInstanceState?.let {
            poiToDelete = it.getParcelable(ARG_POITODELETE) as PointOfInterest?
        }
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(ARG_POITODELETE, poiToDelete)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        poiToDelete = savedInstanceState.getParcelable(ARG_POITODELETE) as PointOfInterest?
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
        Log.v(TAG, "AddFriendDialogListener.sendFriendshipRequest")
        CoroutineScope(Dispatchers.IO).launch {
            Friends.addFriend(username)

            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(this@ListActivity, R.string.friend_request_sent, Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }
    }

    override fun onDeleteButtonPressed(dialog: DialogFragment, poiName: String) {
        Log.v(TAG, "EliminatePointOfInterestDialogListener.onDeleteButtonPressed")
        CoroutineScope(Dispatchers.IO).launch {
            val pois = PointsOfInterest.getPointsOfInterest()
            poiToDelete = pois.first { it.name == poiName } // It exists for sure.
            poiToDelete?.let {
                PointsOfInterest.removePointOfInterestLocally(it)
                CoroutineScope(Dispatchers.Main).launch { dialog.dismiss() }
            }
        }
    }

    override fun onCancelDeletionButtonPressed(dialog: DialogFragment) {
        Log.v(TAG, "EliminatePointOfInterestDialogListener.onCancelDeletionButtonPressed")
        poiToDelete?.let {
            PointsOfInterest.addPointOfInterestLocally(it)
            dialog.dismiss()
        }
        poiToDelete = null
    }

    override fun onDeletionConfirmation(dialog: DialogFragment) {
        Log.v(TAG, "EliminatePointOfInterestDialogListener.onDeletionConfirmation")
        if(poiToDelete == null) {
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            poiToDelete?.let {
                PointsOfInterest.removePointOfInterest(it)
                CoroutineScope(Dispatchers.Main).launch { dialog.dismiss() }
            }
        }
    }

    override fun onPoiSelected(fragment: Fragment, poiName: String) {
        Log.v(TAG, "PointsOfInterestDialogListener.onPoiSelected")
        CoroutineScope(Dispatchers.IO).launch {
            val pois = PointsOfInterest.getPointsOfInterest()
            val selectedPoi = pois.first { it.name == poiName } // It surely exists.
            val poiDetailsDialogFragment = PoiDetailsDialogFragment.newInstance(selectedPoi)
            poiDetailsDialogFragment.show(supportFragmentManager, "PoiDetailsDialogFragment")
        }
    }

    override fun onPointOfInterestSelected(
        dialog: DialogFragment,
        friendPoi: PointOfInterest
    ) {
        Log.v(TAG, "FriendDialogListener.onPointOfInterestSelected")
        val friendPoiDialog = FriendPoiDialogFragment.newInstance(friendPoi)
        dialog.dismiss()
        friendPoiDialog.show(
            supportFragmentManager,
            "FriendPoiDialogFragment"
        )
    }

    override fun removeFriend(dialog: DialogFragment, friendUsername: String) {
        Log.v(TAG, "FriendDialogListener.removeFriend")
        CoroutineScope(Dispatchers.IO).launch {
            Friends.removeFriend(friendUsername)
        }
    }

    override fun onAddButtonPressed(dialog: DialogFragment, poi: PointOfInterest) {
        Log.v(TAG, "FriendPoiDialogListener.onAddButtonPressed")
        CoroutineScope(Dispatchers.IO).launch {
            PointsOfInterest.addPointOfInterest(AddPointOfInterest(AddPointOfInterestPoi(poi)))
            PointsOfInterest.getPointsOfInterest(forceSync = true)
            CoroutineScope(Dispatchers.Main).launch { dialog.dismiss() }
        }
    }

    override fun onRouteButtonPressed(dialog: DialogFragment, address: String) {
        Log.v(TAG, "FriendPoiDialogListener.onRouteButtonPressed")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$address"))
        dialog.dismiss()
        startActivity(intent)
    }
}