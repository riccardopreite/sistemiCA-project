package com.example.maptry.api

import com.example.maptry.model.pointofinterests.AddPointOfInterest
import com.example.maptry.model.pointofinterests.PointOfInterest
import com.example.maptry.model.pointofinterests.RemovePointOfInterest
import retrofit2.Response
import retrofit2.http.*

interface PointOfInterestsApi {
    @GET("/points-of-interest")
    suspend fun getPointsOfInterest(@Query("user") user: String, @Query("friend") friend: String = ""): Response<List<PointOfInterest>>

    @POST("/points-of-interest/add")
    suspend fun addPointOfInterest(@Body addPointOfInterest: AddPointOfInterest): Response<String>

    @HTTP(method = "DELETE", path = "/points-of-interest/remove", hasBody = true)
    suspend fun removePointOfInterest(@Body removePointOfInterest: RemovePointOfInterest): Response<Unit>
}