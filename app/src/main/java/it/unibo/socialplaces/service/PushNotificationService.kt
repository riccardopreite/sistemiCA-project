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
import it.unibo.socialplaces.activity.handler.FriendRequestAcceptedActivity
import it.unibo.socialplaces.activity.handler.LiveEventActivity
import it.unibo.socialplaces.config.PushNotification
import it.unibo.socialplaces.domain.Notification
import it.unibo.socialplaces.model.liveevents.LiveEvent
import it.unibo.socialplaces.model.pointofinterests.PointOfInterest
import it.unibo.socialplaces.receiver.FriendRequestBroadcast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock


class PushNotificationService: FirebaseMessagingService() {
    companion object {
        private val TAG = PushNotificationService::class.qualifiedName

        private var REQUEST_CODE_COUNTER = 0

        // Actions received via notification data fields
        private const val newFriendRequestAction = "new-friend-request"
        private const val friendRequestAcceptedAction = "friend-request-accepted"
        private const val newLiveEventAction = "new-live-event"
        private const val placeRecommendationAction = "place-recommendation"
        private const val modelRetrainedAction = "model-retrained"

        // Actions set in action field in Intents for broadcasting
        private const val acceptFriendshipRequestAction = "accept-friendship-request"
        private const val denyFriendshipRequestAction = "deny-friendship-request"
    }

    private val placeRecommendationNotificationExpectedKeys = listOf("markId", "address", "type", "latitude", "longitude", "name", "phoneNumber", "visibility", "url")
    private val liveEventNotificationExpectedKeys = listOf("id", "address", "latitude", "longitude", "name", "owner", "expirationDate")
    private val friendAcceptedNotificationExpectedKeys = listOf("friendUsername")

    private val defaultSoundUri: Uri = RingtoneManager.getDefaultUri(
            RingtoneManager.TYPE_NOTIFICATION
        )

    @SuppressLint("UnspecifiedImmutableFlag")
    override fun onMessageReceived(message: RemoteMessage) {
        Log.v(TAG, "onMessageReceived")
        super.onMessageReceived(message)
        PushNotification.loadNotificationManager(this)

        val data = message.data
        message.notification?.let {
            // Using the timestamp as notification id.
            val notificationId = Clock.System.now().epochSeconds.toInt()
            when(it.clickAction) {
                newFriendRequestAction -> createFriendRequestNotification(it, notificationId, data)
                friendRequestAcceptedAction -> createFriendAcceptedNotification(it, notificationId, data)
                newLiveEventAction -> createLiveEventNotification(it, notificationId, data)
                placeRecommendationAction -> createPlaceRecommendationNotification(it, notificationId, data)
                modelRetrainedAction -> createModelRetrainedNotification(it, notificationId)
                else -> null
            }?.let { notification ->
                Log.d(TAG, "Showing notification with id=$notificationId.")
                PushNotification.displayNotification(notificationId, notification)
            }
        }
    }

    override fun onNewToken(token: String) {
        Log.v(TAG, "onNewToken")
        super.onNewToken(token)
        PushNotification.uploadNotificationToken(token)
    }

    /**
     * Model retrained just show the new accuracy
     */
    private fun createModelRetrainedNotification(notificationMessage: RemoteMessage.Notification,id: Int): android.app.Notification {
        Log.v(TAG, "createModelRetrainedNotification")
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
    @SuppressLint("UnspecifiedImmutableFlag")
    private fun createPlaceRecommendationNotification(
        notificationMessage: RemoteMessage.Notification,
        id: Int,
        data: Map<String, String>
    ): android.app.Notification? {
        Log.v(TAG, "createPlaceRecommendationNotification")
        if(!data.keys.containsAll(placeRecommendationNotificationExpectedKeys)) {
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

        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            action = "recommendation"
            putExtra("notificationId", id)
            putExtra("place", recommendedPlace)
        }
        val recommendationPending = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

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
        data: Map<String, String>
    ): android.app.Notification? {
        Log.v(TAG, "createPlaceRecommendationNotification")
        if(!data.keys.containsAll(liveEventNotificationExpectedKeys)) {
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
            putExtra("notificationId", id) // Int
            putExtra("liveEvent", liveEvent) // Parcelable
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
        data: Map<String, String>
    ): android.app.Notification? {
        Log.v(TAG, "createFriendAcceptedNotification")
        if(!data.keys.containsAll(friendAcceptedNotificationExpectedKeys)) {
            return null
        }

        val friendUsername = data["friendUsername"]!!

        val friendAcceptedIntent = Intent(this, FriendRequestAcceptedActivity::class.java).apply {
            putExtra("notificationId", id) // Int
            putExtra("friendUsername", friendUsername) // String
        }
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
        data: Map<String, String>
    ): android.app.Notification? {
        Log.v(TAG, "createFriendRequestNotification")
        if(!data.keys.containsAll(friendAcceptedNotificationExpectedKeys)) {
            return null
        }

        val friendUsername = data["friendUsername"]!!

        Log.d(TAG, "Creating notification with id=$id")

        val acceptFriendshipRequestedIntent = Intent(this, FriendRequestBroadcast::class.java).apply {
            putExtra("notificationId", id) // Int
            putExtra("friendUsername", friendUsername) // String
        }.apply { action = acceptFriendshipRequestAction }

        val denyFriendshipRequestedIntent = Intent(this, FriendRequestBroadcast::class.java).apply {
            putExtra("notificationId", id) // Int
            putExtra("friendUsername", friendUsername) // String
        }.apply { action = denyFriendshipRequestAction }

        // ATTENTION: If the intents get modified along the way, the modifications get
        // discarded due to the PendingIntent.FLAG_IMMUTABLE flag.

        val acceptFriendshipRequestedPending = PendingIntent.getBroadcast(
            this,
            0,
            acceptFriendshipRequestedIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val denyFriendshipRequestedPending = PendingIntent.getBroadcast(
            this,
            0,
            denyFriendshipRequestedIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val baseBuilder = baseNotificationBuilder(
            notificationMessage.title!!,
            notificationMessage.body!!,
            android.app.Notification.CATEGORY_SOCIAL
        )

        baseBuilder.addAction(R.drawable.ic_addfriendnotification, getString(R.string.addFriend), acceptFriendshipRequestedPending)
        baseBuilder.addAction(R.drawable.ic_closenotification, getString(R.string.denyFriend), denyFriendshipRequestedPending)

        return baseBuilder.build().apply { flags = android.app.Notification.FLAG_NO_CLEAR }
    }

    /**
     * Returns a builder for notifications with some default configuration
     * (such as title, body, category which are the method's arguments, priority, sound, etc.).
     */
    private fun baseNotificationBuilder(title: String, body: String, category: String): NotificationCompat.Builder {
        return NotificationCompat.Builder(applicationContext, PushNotification.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setCategory(category)
            .setSmallIcon(R.mipmap.ic_social)
            .setPriority(NotificationManager.IMPORTANCE_HIGH)
            .setAutoCancel(false)
            .setSound(defaultSoundUri)
            .setVisibility(VISIBILITY_PUBLIC)
    }


    @SuppressLint("UnspecifiedImmutableFlag")
    private fun createPendingIntent(activityIntent: Intent?): PendingIntent {
        val backIntent = Intent(this, MainActivity::class.java)
        backIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)

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
}