package com.example.maptry.api

import com.example.maptry.model.liveevents.AddLiveEvent
import com.example.maptry.model.liveevents.LiveEvent
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface LiveEventsApi {
    @GET("/live-events")
    suspend fun getLiveEvents(@Query("user") user: String): Response<List<LiveEvent>>

    @POST("/live-events/add")
    suspend fun addLiveEvent(@Body addLiveEvent: AddLiveEvent): Response<Unit>
}