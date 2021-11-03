package it.unibo.socialplaces.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import it.unibo.socialplaces.config.PushNotification.getManager
import it.unibo.socialplaces.domain.Friends.addFriend
import it.unibo.socialplaces.domain.Friends.removeFriend
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
        if (action == "accept"){
            Log.v(TAG,"accept friend request")
            CoroutineScope(Dispatchers.Main).launch {
                addFriend(friendUsername!!)
            }

        }
        else if(action == "deny"){
            /**
             * Not sure about this
             */
            Log.v(TAG,"deny friend request")
            CoroutineScope(Dispatchers.Main).launch {
                removeFriend(friendUsername!!)
            }

        }
        getManager().cancel(notificationId!!)
    }

}
