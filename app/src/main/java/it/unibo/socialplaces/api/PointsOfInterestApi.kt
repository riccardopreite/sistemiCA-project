package it.unibo.socialplaces.api

import it.unibo.socialplaces.model.pointofinterests.AddPointOfInterest
import it.unibo.socialplaces.model.pointofinterests.AddedPointOfInterest
import it.unibo.socialplaces.model.pointofinterests.PointOfInterest
import it.unibo.socialplaces.model.pointofinterests.RemovePointOfInterest
import org.json.JSONObject
import retrofit2.Response
import retrofit2.http.*

interface PointsOfInterestApi {
    @GET("/points-of-interest")
    suspend fun getPointsOfInterest(@Query("user") user: String, @Query("friend") friend: String = ""): Response<List<PointOfInterest>>

    @POST("/points-of-interest/add")
    suspend fun addPointOfInterest(@Body addPointOfInterest: AddPointOfInterest): Response<AddedPointOfInterest>

    @HTTP(method = "DELETE", path = "/points-of-interest/remove", hasBody = true)
    suspend fun removePointOfInterest(@Body removePointOfInterest: RemovePointOfInterest): Response<Unit>
}