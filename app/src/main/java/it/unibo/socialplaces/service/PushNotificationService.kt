package it.unibo.socialplaces.service

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import it.unibo.socialplaces.R
import it.unibo.socialplaces.activity.MainActivity
import it.unibo.socialplaces.config.PushNotification
import it.unibo.socialplaces.model.liveevents.LiveEvent
import it.unibo.socialplaces.model.pointofinterests.PointOfInterest
import kotlinx.datetime.Clock


class PushNotificationService: FirebaseMessagingService() {
    companion object {
        private val TAG = PushNotificationService::class.qualifiedName!!
    }

    /**
     * All the possible keys of a notification of place recommendation.
     * All of them must be present otherwise the notification is not pushed.
     */
    private val placeRecommendationNotificationExpectedKeys = listOf("markId", "address", "type", "latitude", "longitude", "name", "phoneNumber", "visibility", "url")

    /**
     * All the possible keys of a notification of a new live event.
     * All of them must be present otherwise the notification is not pushed.
     */
    private val liveEventNotificationExpectedKeys = listOf("id", "address", "latitude", "longitude", "name", "owner", "expirationDate")

    /**
     * All the possible keys of a notification of a friend request/accepted friend request notifications.
     * All of them must be present otherwise the notification is not pushed.
     */
    private val friendNotificationExpectedKeys = listOf("friendUsername")

    /**
     * Sound for the notification.
     */
    private val defaultSoundUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    @SuppressLint("UnspecifiedImmutableFlag")
    override fun onMessageReceived(message: RemoteMessage) {
        Log.v(TAG, "onMessageReceived")
        super.onMessageReceived(message)

        PushNotification.loadNotificationManager(this)

        val data = message.data
        val notificationId = Clock.System.now().epochSeconds.toInt()

        message.notification?.let {
            val builder = baseNotificationBuilder(it.title!!, it.body!!)
            when(it.clickAction) {
                getString(R.string.notification_new_friend_request) -> createFriendRequestNotification(builder, notificationId, data)
                getString(R.string.notification_friend_request_accepted) -> createFriendAcceptedNotification(builder, data)
                getString(R.string.notification_new_live_event) -> createLiveEventNotification(builder, data)
                getString(R.string.notification_place_recommendation) -> createPlaceRecommendationNotification(builder, data, getString(R.string.activity_place_place_recommendation))
                getString(R.string.notification_validity_recommendation) -> createPlaceRecommendationNotification(builder, data, getString(R.string.activity_place_validity_recommendation))
                getString(R.string.notification_model_retrained) -> createModelRetrainedNotification(builder)
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
     * Generates a notification displaying the level of accuracy reached by the model thanks to the
     * user contribution.
     */
    private fun createModelRetrainedNotification(builder: NotificationCompat.Builder): android.app.Notification {
        Log.v(TAG, "createModelRetrainedNotification")

        builder.setCategory(android.app.Notification.CATEGORY_STATUS)

        return builder.build()
    }

    /**
     * Generates a notification suggesting the user some place (point of interest to visit given
     * its current location, time, day of week and human activity.
     */
    @SuppressLint("UnspecifiedImmutableFlag")
    private fun createPlaceRecommendationNotification(
        builder: NotificationCompat.Builder,
        data: Map<String, String>,
        actionName: String
    ): android.app.Notification? {
        Log.v(TAG, "createPlaceRecommendationNotification $actionName")
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

        val recommendationIntent = Intent(this, MainActivity::class.java).apply {
            action = actionName
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("place", recommendedPlace) // PointOfInterest
            putExtra("notification", true)
        }


        builder.setCategory(android.app.Notification.CATEGORY_RECOMMENDATION)
        builder.setContentIntent(createPendingIntent(recommendationIntent))

        return builder.build()
    }
    /**
     * It could be the same of recommendation
     * Live event should show the poi on the map (Maybe also route button to place?)
     * In theory id is not necessary cause of Autocancel
     */
    @SuppressLint("UnspecifiedImmutableFlag")
    private fun createLiveEventNotification(
        builder: NotificationCompat.Builder,
        data: Map<String, String>
    ): android.app.Notification? {
        Log.v(TAG, "createLiveEventNotification")
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
        val liveEventIntent = Intent(this, MainActivity::class.java).apply {
            action = getString(R.string.activity_new_live_event)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("live", liveEvent) // LiveEvent
            putExtra("notification", true)
        }

        builder.setCategory(android.app.Notification.CATEGORY_RECOMMENDATION)
        builder.setContentIntent(createPendingIntent(liveEventIntent))

        return builder.build()
    }

    /**
     * Show friendList and high light new friend
     * In theory id is not necessary cause of Autocancel
     */
    @SuppressLint("UnspecifiedImmutableFlag")
    private fun createFriendAcceptedNotification(
        builder: NotificationCompat.Builder,
        data: Map<String, String>
    ): android.app.Notification? {
        Log.v(TAG, "createFriendAcceptedNotification")
        if(!data.keys.containsAll(friendNotificationExpectedKeys)) {
            return null
        }

        val friendUsername = data["friendUsername"]!!

        val friendAcceptedIntent = Intent(this, MainActivity::class.java).apply {
            action = getString(R.string.activity_friend_request_accepted)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("friendUsername", friendUsername) // String
            putExtra("notification", true)
        }

        builder.setCategory(android.app.Notification.CATEGORY_SOCIAL)
        builder.setContentIntent(createPendingIntent(friendAcceptedIntent))

        return builder.build()
    }

    /**
     * Onclick disabled just replay only with accept or deny friend request
     * In theory id is not necessary cause of Autocancel
     */
    @SuppressLint("UnspecifiedImmutableFlag")
    private fun createFriendRequestNotification(
        builder: NotificationCompat.Builder,
        id: Int,
        data: Map<String, String>
    ): android.app.Notification? {
        Log.v(TAG, "createFriendRequestNotification")
        if(!data.keys.containsAll(friendNotificationExpectedKeys)) {
            return null
        }

        val friendUsername = data["friendUsername"]!!

        Log.d(TAG, "Creating notification with id=$id")

        val notificationFriendRequestIntent = Intent(this, MainActivity::class.java).apply {
            action = getString(R.string.activity_new_friend_request)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("friendUsername", friendUsername)
            putExtra("notification", true)
        }

        builder.setCategory(android.app.Notification.CATEGORY_SOCIAL)
        builder.setContentIntent(createPendingIntent(notificationFriendRequestIntent))

        return builder.build()
    }

    /**
     * Returns a builder for notifications with some default configuration
     * (such as title, body which are the method's arguments, priority, sound, etc.).
     */
    private fun baseNotificationBuilder(title: String, body: String): NotificationCompat.Builder {
        return NotificationCompat.Builder(applicationContext, PushNotification.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.mipmap.ic_social)
            .setPriority(NotificationManager.IMPORTANCE_HIGH)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setVisibility(VISIBILITY_PUBLIC)
    }

    /**
     * Returns a base [PendingIntent] for launching notifications.
     */
    @SuppressLint("UnspecifiedImmutableFlag")
    private fun createPendingIntent(intent: Intent): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        intent,
        PendingIntent.FLAG_CANCEL_CURRENT
    )
}