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
        private const val TAG = "receiver.FriendRequestBroadcast"

    }
    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent!!.action
        val notificationId = intent.extras?.getInt("notificationId")
        val friendUsername = intent.extras?.getString("friendUsername")
        when (action) {
            "accept" -> {
                Log.i(TAG, "Friend request accepted!")
                CoroutineScope(Dispatchers.Main).launch {
                    Friends.addFriend(friendUsername!!)
                }
            }
            "deny" -> {
                Log.v(TAG, "Friend request denied!")
                CoroutineScope(Dispatchers.Main).launch {
                    Friends.denyFriend(friendUsername!!)
                }
            }
            else -> Unit
        }
        PushNotification.notificationManager.cancel(notificationId!!)
    }

}
