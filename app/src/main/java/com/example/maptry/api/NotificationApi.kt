package com.example.maptry.api

import com.example.maptry.model.notification.NotificationToken
import retrofit2.Response
import retrofit2.http.*

interface NotificationApi {
    @POST("/notification/token")
    suspend fun addNotificationToken(@Body notificationToken: NotificationToken): Response<Unit>
}