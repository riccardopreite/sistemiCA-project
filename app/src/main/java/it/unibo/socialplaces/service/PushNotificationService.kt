package it.unibo.socialplaces.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC
import androidx.core.app.NotificationManagerCompat
import it.unibo.socialplaces.activity.MainActivity
import it.unibo.socialplaces.domain.Notification
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import it.unibo.socialplaces.R
import it.unibo.socialplaces.config.PushNotification.setManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.random.Random
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build


class PushNotificationService: FirebaseMessagingService() {
    companion object {
        private const val TAG = "service.PushNotificationService"

        const val NOTIFICATION_CHANNEL_ID = "it.unibo.push"
        const val CHANNEL_NAME = "SocialPlaces : Push notification"
    }
    private lateinit var manager :NotificationManager
    private val channel = NotificationChannel(
        NOTIFICATION_CHANNEL_ID,
        CHANNEL_NAME,
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        description = "channel for user notification"
        lightColor = Color.BLUE
        lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        enableVibration(true)
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    override fun onMessageReceived(message: RemoteMessage) {
        Log.v(TAG, "onMessageReceived")
        super.onMessageReceived(message)
        manager = setManager(getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        manager.createNotificationChannel(channel)

        message.notification?.let {
            val intent = when(it.clickAction) {
                "new-friend-request" -> {
                    Intent(this, MainActivity::class.java) // TODO Sostituire con corretti intent
                }
                "friend-request-accepted" -> {
                    Intent(this, MainActivity::class.java) // TODO Sostituire con corretti intent
                }
                "new-live-event" -> {
                    Intent(this, MainActivity::class.java) // TODO Sostituire con corretti intent
                }
                "place-recommendation" -> {
                    Intent(this, MainActivity::class.java) // TODO Sostituire con corretti intent
                }
                "model-retrained" -> {
                    Intent(this, MainActivity::class.java) // TODO Sostituire con corretti intent
                }
                else -> {
                    Intent(this, MainActivity::class.java) // TODO Sostituire con corretti intent
                } // TODO Capire quali altri casi ci sono, per ora ho messo solo quelli di sopra
            }.apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)

            val defaultSoundUri: Uri =
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val builder = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_social)
                .setContentTitle(it.title)
                .setContentText(it.body)
                .setPriority(NotificationManager.IMPORTANCE_HIGH)
                .setContentIntent(pendingIntent)
                .setCategory(android.app.Notification.CATEGORY_RECOMMENDATION)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setVisibility(VISIBILITY_PUBLIC)

                .build()


            with(NotificationManagerCompat.from(this)) {
                val id = System.currentTimeMillis().toInt().absoluteValue
                Log.v(TAG, "NOTIFY WITH COUNTER $id")
                notify(id, builder)
            }

        }
    }

    override fun onNewToken(token: String) {
        Log.v(TAG, "onNewToken")
        super.onNewToken(token)
        CoroutineScope(Dispatchers.IO).launch {
            Notification.addNotificationToken(token)
            Log.i(TAG, "The Firebase Messaging token was updated.")
        }
    }
}