package it.unibo.socialplaces.config

import android.util.Log
import it.unibo.socialplaces.domain.Notification
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object PushNotification {
    private const val TAG = "PushNotification"

    fun loadNotificationManager() {
        Log.v(TAG, "loadNotificationManager")

        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            CoroutineScope(Dispatchers.IO).launch {
                Notification.addNotificationToken(token)
            }
        }
    }
}