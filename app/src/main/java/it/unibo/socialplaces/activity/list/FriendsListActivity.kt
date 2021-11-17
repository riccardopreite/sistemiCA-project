package it.unibo.socialplaces.activity.list

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import it.unibo.socialplaces.R
import it.unibo.socialplaces.domain.Friends
import it.unibo.socialplaces.domain.PointsOfInterest
import it.unibo.socialplaces.fragment.FriendsFragment
import it.unibo.socialplaces.fragment.dialog.friends.AddFriendDialogFragment
import it.unibo.socialplaces.fragment.dialog.friends.EliminateFriendDialogFragment
import it.unibo.socialplaces.fragment.dialog.friends.FriendDialogFragment
import it.unibo.socialplaces.fragment.dialog.friends.FriendPoiDialogFragment
import it.unibo.socialplaces.model.friends.Friend
import it.unibo.socialplaces.model.pointofinterests.AddPointOfInterest
import it.unibo.socialplaces.model.pointofinterests.AddPointOfInterestPoi
import it.unibo.socialplaces.model.pointofinterests.PointOfInterest
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FriendsListActivity: it.unibo.socialplaces.activity.ListActivity(),
    AddFriendDialogFragment.AddFriendDialogListener,
    FriendDialogFragment.FriendDialogListener,
    FriendPoiDialogFragment.FriendPoiDialogListener,
    EliminateFriendDialogFragment.EliminateFriendDialogListener {

    companion object {
        private val TAG: String = FriendsListActivity::class.qualifiedName!!

        private const val ARG_FRIENDTODELETE = "friendToDelete"
    }

    // App state
    private var friendToDelete: Friend? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "onCreate")
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

    /**
     * @see AddFriendDialogFragment.AddFriendDialogListener.sendFriendshipRequest
     */
    override fun sendFriendshipRequest(dialog: DialogFragment, username: String) {
        Log.v(TAG, "AddFriendDialogFragment.AddFriendDialogListener.sendFriendshipRequest")
        CoroutineScope(Dispatchers.IO).launch {
            Friends.addFriend(username)

            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(this@FriendsListActivity, R.string.friend_request_sent, Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }
    }

    /**
     * @see FriendDialogFragment.FriendDialogListener.onPointOfInterestSelected
     */
    override fun onPointOfInterestSelected(dialog: DialogFragment, friendPoi: PointOfInterest) {
        Log.v(TAG, "FriendDialogListener.onPointOfInterestSelected")
        val friendPoiDialog = FriendPoiDialogFragment.newInstance(friendPoi)
        dialog.dismiss()
        friendPoiDialog.show(
            supportFragmentManager,
            "FriendPoiDialogFragment"
        )
    }

    /**
     * @see FriendDialogFragment.FriendDialogListener.removeFriend
     */
    override fun removeFriend(dialog: DialogFragment, friendUsername: String) {
        Log.v(TAG, "FriendDialogFragment.FriendDialogListener.removeFriend")
        CoroutineScope(Dispatchers.IO).launch {
            Friends.removeFriend(friendUsername)
            CoroutineScope(Dispatchers.Main).launch { dialog.dismiss() }
        }
    }

    /**
     * @see FriendPoiDialogFragment.FriendPoiDialogListener.onAddButtonPressed
     */
    override fun onAddButtonPressed(dialog: DialogFragment, poi: PointOfInterest) {
        Log.v(TAG, "FriendPoiDialogListener.onAddButtonPressed")
        CoroutineScope(Dispatchers.IO).launch {
            PointsOfInterest.addPointOfInterest(AddPointOfInterest(AddPointOfInterestPoi(poi)))
            PointsOfInterest.getPointsOfInterest(forceSync = true)
            CoroutineScope(Dispatchers.Main).launch { dialog.dismiss() }
        }
    }

    /**
     * @see FriendPoiDialogFragment.FriendPoiDialogListener.onRouteButtonPressed
     */
    override fun onRouteButtonPressed(dialog: DialogFragment, address: String) {
        Log.v(TAG, "FriendPoiDialogListener.onRouteButtonPressed")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$address"))
        dialog.dismiss()
        startActivity(intent)
    }

    /**
     * @see EliminateFriendDialogFragment.EliminateFriendDialogListener.onDeleteButtonPressed
     */
    override fun onDeleteButtonPressed(dialog: DialogFragment, friendUsername: String) {
        Log.v(TAG, "EliminateFriendDialogFragment.EliminateFriendDialogListener.onDeleteButtonPressed")
        CoroutineScope(Dispatchers.IO).launch {
            val friends = Friends.getFriends(true)
            friendToDelete = friends.first { it.friendUsername == friendUsername } // It exists for sure.
            friendToDelete?.let {
                Friends.removeFriendLocally(it)
                CoroutineScope(Dispatchers.Main).launch { dialog.dismiss() }
            }
        }
    }

    /**
     * @see EliminateFriendDialogFragment.EliminateFriendDialogListener.onCancelDeletionButtonPressed
     */
    override fun onCancelDeletionButtonPressed(dialog: DialogFragment) {
        Log.v(TAG, "EliminateFriendDialogFragment.EliminateFriendDialogListener.onCancelDeletionButtonPressed")
        friendToDelete?.let {
            Friends.addFriendLocally(it)
            dialog.dismiss()
        }
        friendToDelete = null
    }

    /**
     * @see EliminateFriendDialogFragment.EliminateFriendDialogListener.onDeletionConfirmation
     */
    override fun onDeletionConfirmation(dialog: DialogFragment) {
        Log.v(TAG, "EliminateFriendDialogFragment.EliminateFriendDialogListener.onDeletionConfirmation")

        friendToDelete?.let { friend ->
            CoroutineScope(Dispatchers.IO).launch {
                Friends.removeFriend(friend.friendUsername)
                CoroutineScope(Dispatchers.Main).launch {
                    dialog.dismiss()
                    updateFriendList()
                }
            }
        } ?: run {
            Log.w(TAG, "Removal of friend cancelled.")
        }
    }

    /**
     * Updates the friend list calling the SocialPlaces API and, if a loading snackbar
     * was pushed, dismisses the snackbar when the update is completed.
     */
    private fun updateFriendList(snackbar: Snackbar? = null) {
        Log.v(TAG,"updateFriendList")

        CoroutineScope(Dispatchers.IO).launch {
            val friendList = Friends.getFriends(forceSync = true)
            val listFragment = FriendsFragment.newInstance(friendList)
            pushFragment(listFragment)
            CoroutineScope(Dispatchers.Main).launch { snackbar?.dismiss() }
        }
    }
}