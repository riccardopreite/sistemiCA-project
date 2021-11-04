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
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.v(TAG, "onReceive")
        val action = intent!!.action
        val notificationId = intent.getIntExtra("notificationId", -1)
        val friendUsername = intent.getStringExtra("friendUsername")
        Log.d(TAG, "Handling notification with id=$notificationId.")

        CoroutineScope(Dispatchers.Main).launch {
            PushNotification.notificationManager.cancel(notificationId)
        }
        when (action) {
            "accept" -> {
                Log.i(TAG, "Friend request accepted!")
                CoroutineScope(Dispatchers.IO).launch {
                    Friends.confirmFriend(friendUsername!!)
                }
            }
            "deny" -> {
                Log.v(TAG, "Friend request denied!")
                CoroutineScope(Dispatchers.IO).launch {
                    Friends.denyFriend(friendUsername!!)
                }
            }
            else -> Unit
        }
    }

}
