package it.unibo.socialplaces.api

import it.unibo.socialplaces.model.notification.NotificationToken
import retrofit2.Response
import retrofit2.http.*
import it.unibo.socialplaces.model.notification.PublicKey

interface UserDataApi {
    @POST("/user-data/notification-token")
    suspend fun addNotificationToken(@Body notificationToken: NotificationToken): Response<Unit>

    @POST("/user-data/public-key")
    suspend fun addPublicKey(@Body publicKey: PublicKey): Response<Unit>
}