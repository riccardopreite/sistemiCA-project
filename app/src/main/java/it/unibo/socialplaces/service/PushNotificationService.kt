package it.unibo.socialplaces.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import it.unibo.socialplaces.R
import it.unibo.socialplaces.activity.MainActivity
import it.unibo.socialplaces.activity.notification.*
import it.unibo.socialplaces.config.PushNotification
import it.unibo.socialplaces.domain.Notification
import it.unibo.socialplaces.model.liveevents.LiveEvent
import it.unibo.socialplaces.model.pointofinterests.PointOfInterest
import it.unibo.socialplaces.receiver.FriendRequestBroadcast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue


class PushNotificationService: FirebaseMessagingService() {
    companion object {
        private const val TAG = "service.PushNotificationService"

        const val NOTIFICATION_CHANNEL_ID = "it.unibo.socialplaces.pushnotification"
        const val CHANNEL_NAME = "SocialPlaces: Push notification"

        private var REQUEST_CODE_COUNTER = 0

        private const val newFriendRequestAction = "new-friend-request"
        private const val friendRequestAcceptedAction = "friend-request-accepted"
        private const val newLiveEventAction = "new-live-event"
        private const val placeRecommendationAction = "place-recommendation"
        private const val modelRetrainedAction = "model-retrained"
    }

    private val channel = NotificationChannel(
        NOTIFICATION_CHANNEL_ID,
        CHANNEL_NAME,
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        description = "Channel for push notifications."
        lightColor = Color.BLUE
        lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        enableVibration(true)
    }

    private val defaultSoundUri: Uri = RingtoneManager.getDefaultUri(
            RingtoneManager.TYPE_NOTIFICATION
        )

    @SuppressLint("UnspecifiedImmutableFlag")
    override fun onMessageReceived(message: RemoteMessage) {
        Log.v(TAG, "onMessageReceived")
        super.onMessageReceived(message)
        PushNotification.notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        PushNotification.notificationManager.createNotificationChannel(channel)

        val data = message.data
        message.notification?.let {
            val id = System.currentTimeMillis().toInt().absoluteValue
            when(it.clickAction) {
                newFriendRequestAction -> createFriendRequestNotification(it, id, data)
                friendRequestAcceptedAction -> createFriendAcceptedNotification(it, id, data)
                newLiveEventAction -> createLiveEventNotification(it, id, data)
                placeRecommendationAction -> createPlaceRecommendationNotification(it, id, data)
                modelRetrainedAction -> createModelRetrainedNotification(it, id)
                else -> null
            }?.let {
                with(NotificationManagerCompat.from(this)) {
                    Log.d(TAG, "Showing notification with id=$id.")
                    notify(id, it)
                }
            }
        }
    }

    /**
     * Model retrained just show the new accuracy
     */
    private fun createModelRetrainedNotification(notificationMessage: RemoteMessage.Notification,id: Int): android.app.Notification {
        val baseBuilder = baseNotificationBuilder(
            notificationMessage.title!!,
            notificationMessage.body!!,
            android.app.Notification.CATEGORY_STATUS
        )
        return baseBuilder.build()
    }

    /**
     * It could be the same of live event
     * Place recommendation should show the poi on the map (Maybe also route button to place?)
     * In theory id is not necessary cause of Autocancel
     */
    private fun createPlaceRecommendationNotification(
        notificationMessage: RemoteMessage.Notification,
        id: Int,
        data: MutableMap<String, String>
    ): android.app.Notification? {
        if(!data.keys.containsAll(
                listOf("markId", "address", "type", "latitude", "longitude", "name", "phoneNumber", "visibility", "url")
            )
        ) {
            return null
        }

        val recommendedPlace = PointOfInterest(
            data["markId"]!!,
            data["address"]!!,
            data["type"]!!,
            data["latitude"]!!.toDouble(),
            data["longitude"]!!.toDouble(),
            data["name"]!!,
            data["phoneNumber"]!!,
            data["visibility"]!!,
            data["url"]!!,
        )

        val placeRecommendationIntent = Intent(this, PlaceRecommendationActivity::class.java)
        placeRecommendationIntent.putExtra("notificationId",id)
        placeRecommendationIntent.putExtra("place",recommendedPlace)
        val recommendationPending = createPendingIntent(placeRecommendationIntent)

        val baseBuilder = baseNotificationBuilder(
            notificationMessage.title!!,
            notificationMessage.body!!,
            android.app.Notification.CATEGORY_RECOMMENDATION
        )

        baseBuilder.setContentIntent(recommendationPending)

        return baseBuilder.build()
    }
    /**
     * It could be the same of recommendation
     * Live event should show the poi on the map (Maybe also route button to place?)
     * In theory id is not necessary cause of Autocancel

     */
    private fun createLiveEventNotification(
        notificationMessage: RemoteMessage.Notification,
        id: Int,
        data: MutableMap<String, String>
    ): android.app.Notification? {
        if(!data.keys.containsAll(
                listOf("id", "address", "latitude", "longitude", "name", "owner", "expirationDate")
            )
        ) {
            return null
        }

        val liveEvent = LiveEvent(
            data["id"]!!,
            data["address"]!!,
            data["latitude"]!!.toDouble(),
            data["longitude"]!!.toDouble(),
            data["name"]!!,
            data["owner"]!!,
            data["expirationDate"]!!.toLong()
        )

        val liveEventIntent = Intent(this, LiveEventActivity::class.java).apply {
            putExtra("notificationId",id)
            putExtra("liveEvent",liveEvent)
        }
        val livePending = createPendingIntent(liveEventIntent)

        val baseBuilder = baseNotificationBuilder(
            notificationMessage.title!!,
            notificationMessage.body!!,
            android.app.Notification.CATEGORY_RECOMMENDATION
        )

        baseBuilder.setContentIntent(livePending)

        return baseBuilder.build()
    }

    /**
     * Show friendList and high light new friend
     * In theory id is not necessary cause of Autocancel

     */
    private fun createFriendAcceptedNotification(
        notificationMessage: RemoteMessage.Notification,
        id: Int,
        data: MutableMap<String, String>
    ): android.app.Notification? {
        if(!data.keys.contains("friendUsername")) {
            return null
        }

        val friendUsername = data["friendUsername"]!!

        val friendAcceptedIntent = Intent(this, FriendAcceptedActivity::class.java)
        friendAcceptedIntent.putExtra("notificationId",id)
        friendAcceptedIntent.putExtra("friendUsername",friendUsername)
        val friendPending = createPendingIntent(friendAcceptedIntent)


        val baseBuilder = baseNotificationBuilder(
            notificationMessage.title!!,
            notificationMessage.body!!,
            android.app.Notification.CATEGORY_SOCIAL
        )

        baseBuilder.setContentIntent(friendPending)

        return baseBuilder.build()
    }

    /**
     * Onclick disabled just replay only with accept or deny friend request
     * In theory id is not necessary cause of Autocancel
     */
    private fun createFriendRequestNotification(
        notificationMessage: RemoteMessage.Notification,
        id: Int,
        data: MutableMap<String, String>
    ): android.app.Notification? {
        if(!data.keys.contains("friendUsername")) {
            return null
        }

        val friendUsername = data["friendUsername"]!!

        val friendRequestedIntent = Intent(this, FriendRequestBroadcast::class.java)

        friendRequestedIntent.action="accept"
        friendRequestedIntent.putExtra("notificationId",id)
        friendRequestedIntent.putExtra("friendUsername",friendUsername)
        val friendRequestAcceptedPending = PendingIntent.getActivity(this,0,friendRequestedIntent,0)

        friendRequestedIntent.action="deny"
        friendRequestedIntent.putExtra("notificationId",id)
        friendRequestedIntent.putExtra("friendUsername",friendUsername)
        val friendRequestDeniedPending = PendingIntent.getActivity(this,0,friendRequestedIntent,0)

        val baseBuilder = baseNotificationBuilder(
            notificationMessage.title!!,
            notificationMessage.body!!,
            android.app.Notification.CATEGORY_SOCIAL
        )

        val notification = baseBuilder
            .setAutoCancel(false)
            .addAction(R.drawable.ic_addfriendnotification,getString(R.string.addFriend),friendRequestAcceptedPending)
            .addAction(R.drawable.ic_closenotification,getString(R.string.denyFriend),friendRequestDeniedPending)
            .build()

        notification.flags = android.app.Notification.FLAG_NO_CLEAR

        return notification
    }

    private fun baseNotificationBuilder(title: String, body: String, category: String): NotificationCompat.Builder {
        return NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setCategory(category)
            .setSmallIcon(R.mipmap.ic_social)
            .setPriority(NotificationManager.IMPORTANCE_HIGH)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setVisibility(VISIBILITY_PUBLIC)
    }


    @SuppressLint("UnspecifiedImmutableFlag")
    private fun createPendingIntent(activityIntent: Intent?): PendingIntent {
        val backIntent = Intent(this, MainActivity::class.java)
        backIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val arrayOfIntent =
            if (activityIntent != null)
                arrayOf(backIntent, activityIntent)
            else
                arrayOf(backIntent)

        return PendingIntent.getActivities(
            this,
            REQUEST_CODE_COUNTER++,
            arrayOfIntent,
            PendingIntent.FLAG_ONE_SHOT
        )
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