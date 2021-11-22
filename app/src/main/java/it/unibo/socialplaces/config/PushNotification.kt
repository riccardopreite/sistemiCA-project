package it.unibo.socialplaces.config

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.util.Log
import it.unibo.socialplaces.domain.Notification
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object PushNotification {
    private val TAG = PushNotification::class.qualifiedName!!

    const val NOTIFICATION_CHANNEL_ID = "it.unibo.socialplaces.pushnotification"
    private const val CHANNEL_NAME = "SocialPlaces: Push notification"

    private var manager: NotificationManager? = null

    private var notificationManager: NotificationManager
        get() = manager!!
        set(value) {
            if(manager == null) {
                manager = value
            }
        }

    /**
     * Initial setup of the Push Notification managing configuration.
     */
    fun setupNotificationToken() {
        Log.v(TAG, "loadNotificationManager")

        FirebaseMessaging.getInstance().token.addOnSuccessListener(this::uploadNotificationToken)
    }

    /**
     * Retrieves the instance of the `NotificationManager` service from the context and sets the channel.
     */
    fun loadNotificationManager(context: Context) {
        notificationManager = context.getSystemService(FirebaseMessagingService.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(pushNotificationChannel())
    }

    /**
     * Uploads the new [token] to the persistent storage of Social Places API.
     */
    fun uploadNotificationToken(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            Notification.addNotificationToken(token)
        }
    }

    /**
     * Displays a [notification] with identifier [id] (useful to retrieve it for canceling)-
     */
    fun displayNotification(id: Int, notification: android.app.Notification) {
        notificationManager.notify(id, notification)
    }

    /*
    /**
     * Cancel a previously set notification with identifier = [id].
     */
    fun cancelNotification(id: Int) {
        notificationManager.cancel(id)
    }

    /**
     * Returns `true` if the notification with identifier = [id] is present in the list of displayed
     * notifications.
     */
    fun existsNotification(id: Int): Boolean {
        Log.d(TAG, "Active notifications' identifiers: ${notificationManager.activeNotifications.map {it.id }}")
        Log.d(TAG, "Received id: $id")
        return notificationManager.activeNotifications.any { it.id == id }
    }
     */

    /**
     * Returns the channel for publishing notifications after FCM receives one.
     */
    private fun pushNotificationChannel(): NotificationChannel {
        return NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Channel for push notifications."
            lightColor = Color.BLUE
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            enableVibration(true)
        }
    }
}