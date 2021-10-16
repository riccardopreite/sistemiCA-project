package com.example.maptry.activity.list

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.maptry.R
import com.example.maptry.activity.ListActivity
import com.example.maptry.domain.Friends
import com.example.maptry.domain.PointsOfInterest
import com.example.maptry.fragment.FriendsFragment
import com.example.maptry.fragment.dialog.friends.AddFriendDialogFragment
import com.example.maptry.fragment.dialog.friends.EliminateFriendDialogFragment
import com.example.maptry.fragment.dialog.friends.FriendDialogFragment
import com.example.maptry.fragment.dialog.friends.FriendPoiDialogFragment
import com.example.maptry.model.friends.Friend
import com.example.maptry.model.pointofinterests.AddPointOfInterest
import com.example.maptry.model.pointofinterests.AddPointOfInterestPoi
import com.example.maptry.model.pointofinterests.PointOfInterest
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FriendsListActivity: ListActivity(),
    AddFriendDialogFragment.AddFriendDialogListener,
    FriendDialogFragment.FriendDialogListener,
    FriendPoiDialogFragment.FriendPoiDialogListener,
    EliminateFriendDialogFragment.EliminateFriendDialogListener  {
    companion object {
        private val TAG: String = FriendsListActivity::class.qualifiedName!!

        private const val ARG_FRIENDTODELETE = "friendToDelete"
    }

    // App state
    private var friendToDelete: Friend? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let {
            friendToDelete = it.getParcelable(ARG_FRIENDTODELETE) as Friend?
        }
        // Using findViewById(android.R.id.content) is a workaround for accessing a view instance
        val snackbar = Snackbar.make(findViewById(android.R.id.content), R.string.loading_friends, Snackbar.LENGTH_INDEFINITE)
        snackbar.show()
        updateFriendList(snackbar)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(ARG_FRIENDTODELETE, friendToDelete)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        friendToDelete = savedInstanceState.getParcelable(ARG_FRIENDTODELETE) as Friend?
    }

    override fun sendFriendshipRequest(dialog: DialogFragment, username: String) {
        Log.v(TAG, "AddFriendDialogListener.sendFriendshipRequest")
        CoroutineScope(Dispatchers.IO).launch {
            Friends.addFriend(username)

            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(this@FriendsListActivity, R.string.friend_request_sent, Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
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
            CoroutineScope(Dispatchers.Main).launch { dialog.dismiss() }

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

    override fun onDeleteButtonPressed(dialog: DialogFragment, friendUsername: String) {
        Log.v(TAG, "EliminateFriendDialogListener.onDeleteButtonPressed")
        CoroutineScope(Dispatchers.IO).launch {
            val friends = Friends.getFriends()
            friendToDelete = friends.first { it.friendUsername == friendUsername } // It exists for sure.
            friendToDelete?.let {
                Friends.removeFriendLocally(it)
                CoroutineScope(Dispatchers.Main).launch { dialog.dismiss() }
            }
        }
    }

    override fun onCancelDeletionButtonPressed(dialog: DialogFragment) {
        Log.v(TAG, "EliminateFriendDialogListener.onCancelDeletionButtonPressed")
        friendToDelete?.let {
            Friends.addFriendLocally(it)
            dialog.dismiss()
        }
        friendToDelete = null
    }

    override fun onDeletionConfirmation(dialog: DialogFragment) {
        Log.v(TAG, "EliminateFriendDialogListener.onDeletionConfirmation")
        if(friendToDelete == null) {
            Log.w(TAG, "Friends Deletion canceled")

            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            friendToDelete?.let {
                Friends.removeFriend(it.friendUsername)
                CoroutineScope(Dispatchers.Main).launch {
                    dialog.dismiss()
                    updateFriendList()
                }
            }
        }

    }
    private fun updateFriendList(snackbar: Snackbar?=null){
        Log.v(TAG,"FriendListActivity.updateFriendList")

        CoroutineScope(Dispatchers.IO).launch {
            val friendList = Friends.getFriends(forceSync = true)
            val listFragment = FriendsFragment.newInstance(friendList)
            pushFragment(listFragment)
            CoroutineScope(Dispatchers.Main).launch { snackbar?.dismiss() }
        }
    }
}