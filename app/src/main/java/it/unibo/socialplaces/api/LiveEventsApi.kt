package it.unibo.socialplaces.api

import it.unibo.socialplaces.model.liveevents.AddLiveEvent
import it.unibo.socialplaces.model.liveevents.LiveEvent
import org.json.JSONObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface LiveEventsApi {
    @GET("/live-events")
    suspend fun getLiveEvents(@Query("user") user: String): Response<List<LiveEvent>>

    @POST("/live-events/add")
    suspend fun addLiveEvent(@Body addLiveEvent: AddLiveEvent): Response<JSONObject>
}