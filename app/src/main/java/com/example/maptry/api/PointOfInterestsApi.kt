package com.example.maptry.api

import com.example.maptry.model.pointofinterests.AddPointOfInterest
import com.example.maptry.model.pointofinterests.AddPointOfInterestPoi
import com.example.maptry.model.pointofinterests.PointOfInterest
import com.example.maptry.model.pointofinterests.RemovePointOfInterest
import okhttp3.internal.http.hasBody
import retrofit2.Response
import retrofit2.http.*

interface PointOfInterestsApi {
    @GET("/points-of-interest") // TODO Manca un Response<String> per i status code 400
    suspend fun getPointsOfInterest(@Query("user") user: String, @Query("friend") friend: String = ""): Response<List<PointOfInterest>>

    @POST("/points-of-interest/add")
    suspend fun addPointOfInterest(@Body addPointOfInterest: AddPointOfInterest): Response<String>

    @HTTP(method = "DELETE", path = "/points-of-interest/remove", hasBody = true)
    suspend fun removePointOfInterest(@Body removePointOfInterest: RemovePointOfInterest): Response<Unit>
}