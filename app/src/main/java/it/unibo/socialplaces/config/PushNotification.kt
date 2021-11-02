package it.unibo.socialplaces.config

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import it.unibo.socialplaces.domain.Notification
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object PushNotification {
    private const val TAG = "PushNotification"

    private lateinit var manager: NotificationManager

    fun getManager(): NotificationManager {
        return this.manager
    }
    fun setManager(manager: NotificationManager):NotificationManager{
        if (!this::manager.isInitialized)
            this.manager = manager
        return getManager()
    }
    fun loadNotificationManager() {
        Log.v(TAG, "loadNotificationManager")

        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            CoroutineScope(Dispatchers.IO).launch {
                Notification.addNotificationToken(token)
            }
        }
    }
}