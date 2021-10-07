package com.example.maptry.api

import com.example.maptry.model.notification.NotificationToken
import retrofit2.Response
import retrofit2.http.*

interface NotificationApi {
   /* @GET("/points-of-interest") // TODO Manca un Response<String> per i status code 400
    suspend fun getPointsOfInterest(@Query("user") user: String, @Query("friend") friend: String = ""): Response<List<PointOfInterest>>

    @POST("/points-of-interest/add")
    suspend fun addPointOfInterest(@Body addPointOfInterest: AddPointOfInterest): Response<String>
*/
    @HTTP(method = "POST", path = "/notification/token", hasBody = true)
    suspend fun addNotificationToken(@Body token: NotificationToken): Response<Unit>
}