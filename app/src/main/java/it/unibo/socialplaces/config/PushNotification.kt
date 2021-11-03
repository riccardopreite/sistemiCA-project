package it.unibo.socialplaces.config

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat.getSystemService
import it.unibo.socialplaces.domain.Notification
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object PushNotification {
    private const val TAG = "PushNotification"

    private var manager: NotificationManager? = null

    var notificationManager: NotificationManager
        get() = manager!!
        set(value) {
            if(manager == null) {
                manager = value;
            }
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