package it.unibo.socialplaces.domain

import android.util.Log
import it.unibo.socialplaces.api.ApiConnectors
import it.unibo.socialplaces.api.ApiError
import it.unibo.socialplaces.model.recommendation.PlaceRequest
import it.unibo.socialplaces.model.recommendation.ValidationRequest
import okhttp3.ResponseBody
import retrofit2.HttpException
import java.io.IOException

object Recommendation {
    private val TAG = Recommendation::class.qualifiedName!!

    private val api by lazy {
        ApiConnectors.recommendationApi
    }

    private val handleApiError: (ResponseBody?) -> ApiError = ApiConnectors::handleApiError

    private lateinit var userId: String

    /**
     * Sets the default user (logged in user) for API calls.
     * @param user the logged in user.
     */
    fun setUserId(user: String) {
        Log.v(TAG, "setUserId")
        userId = user
    }

    /**
     * Calls POST /recommendation/train in the SocialPlaces API.
     * @see it.unibo.socialplaces.api.RecommendationApi.trainModel
     * @param trainRequest the data to retrain the model in the SocialPlaces Context-Aware system.
     */
    suspend fun trainModel(trainRequest: ValidationRequest) {
        Log.v(TAG, "trainModel")

        val response = try {
            api.trainModel(trainRequest.copy(user = userId))
        } catch (e: IOException) {
            e.message?.let { Log.e(TAG, it) }
            return
        } catch (e: HttpException) {
            e.message?.let { Log.e(TAG, it) }
            return
        }

        if(response.isSuccessful) {
            Log.i(TAG, "Model trained successfully.")
        } else {
            Log.e(TAG, handleApiError(response.errorBody()).toString())
        }
    }

    /**
     * Calls POST /recommendation/validity in the SocialPlaces API.
     * @see it.unibo.socialplaces.api.RecommendationApi.validityPlace
     * @param validityRequest the data of a location, a time in the day and week, a human activity,
     * a place category to see if they match together.
     */
    suspend fun validityPlace(validityRequest: ValidationRequest) {
        Log.v(TAG, "validityPlace")

        val response = try {
            api.validityPlace(validityRequest.copy(user = userId))
        } catch (e: IOException) {
            e.message?.let { Log.e(TAG, it) }
            return
        } catch (e: HttpException) {
            e.message?.let { Log.e(TAG, it) }
            return
        }

        if(response.isSuccessful) {
            Log.i(TAG, "Place validity request sent successfully.")
        } else {
            Log.e(TAG, handleApiError(response.errorBody()).toString())
        }
    }

    /**
     * Calls POST /recommendation/places in the SocialPlaces API.
     * @see it.unibo.socialplaces.api.RecommendationApi.recommendPlace
     * @param placeRequest the data of a location, a time in the day and week, a human activity.
     */
    suspend fun recommendPlace(placeRequest: PlaceRequest) {
        Log.v(TAG, "recommendPlace")

        val response = try {
            api.recommendPlace(placeRequest.copy(user = userId))
        } catch (e: IOException) {
            e.message?.let { Log.e(TAG, it) }
            return
        } catch (e: HttpException) {
            e.message?.let { Log.e(TAG, it) }
            return
        }

        if(response.isSuccessful) {
            Log.i(TAG, "Place recommendation request sent successfully.")
        } else {
            Log.e(TAG, handleApiError(response.errorBody()).toString())
        }
    }
}

