package it.unibo.socialplaces.api

import it.unibo.socialplaces.model.notification.NotificationToken
import retrofit2.Response
import retrofit2.http.*

interface NotificationApi {
    @POST("/notification/token")
    suspend fun addNotificationToken(@Body notificationToken: NotificationToken): Response<Unit>
}