package it.unibo.socialplaces.api

import it.unibo.socialplaces.model.notification.NotificationToken
import it.unibo.socialplaces.model.recommendation.PlaceRequest
import it.unibo.socialplaces.model.recommendation.ValidationRequest
import retrofit2.Response
import retrofit2.http.*

interface RecommendationApi {
    @POST("/recommendation/train")
    suspend fun trainModel(@Body trainRequest: ValidationRequest): Response<Unit>

    @POST("/recommendation/validity")
    suspend fun validityPlace(@Body validationRequest: ValidationRequest): Response<Unit>

    @POST("/recommendation/places")
    suspend fun recommendPlace(@Body placeRequest: PlaceRequest): Response<Unit>
}