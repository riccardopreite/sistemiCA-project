package it.unibo.socialplaces.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import it.unibo.socialplaces.config.PushNotification
import it.unibo.socialplaces.domain.Friends
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FriendRequestBroadcast: BroadcastReceiver() {
    companion object{
        private val TAG = FriendRequestBroadcast::class.qualifiedName!!

        // Actions received via action field in intent.
        private const val acceptFriendshipRequestAction = "accept-friendship-request"
        private const val denyFriendshipRequestAction = "deny-friendship-request"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.v(TAG, "onReceive")
        if(intent == null) {
            Log.e(TAG, "Intent is null!")
            return
        }

        val notificationId = intent.getIntExtra("notificationId", -1)
        val friendUsername = intent.getStringExtra("friendUsername") ?: ""
        Log.d(TAG, "Handling notification with id=$notificationId.")

        PushNotification.cancelNotification(notificationId)

        when (intent.action) {
            acceptFriendshipRequestAction -> {
                CoroutineScope(Dispatchers.IO).launch {
                    Friends.confirmFriend(friendUsername)
                    Log.i(TAG, "Friend request accepted!")
                }
            }
            denyFriendshipRequestAction -> {
                CoroutineScope(Dispatchers.IO).launch {
                    Friends.denyFriend(friendUsername)
                    Log.v(TAG, "Friend request denied!")
                }
            }
            else -> Unit
        }
    }

}
