package it.unibo.socialplaces.domain

import android.util.Log
import it.unibo.socialplaces.api.RetrofitInstances
import it.unibo.socialplaces.model.notification.NotificationToken
import it.unibo.socialplaces.model.pointofinterests.PointOfInterest
import it.unibo.socialplaces.model.pointofinterests.RemovePointOfInterest
import it.unibo.socialplaces.model.recommendation.PlaceRequest
import it.unibo.socialplaces.model.recommendation.ValidationRequest
import retrofit2.HttpException
import java.io.IOException

object Recommendation {
    private const val TAG = "domain.Recommendation"

    private val api by lazy {
        RetrofitInstances.recommendationApi
    }

    private lateinit var userId: String

    fun setUserId(user: String) {
        Log.v(TAG, "setUserId")
        userId = user
    }

    suspend fun trainModel(trainRequest: ValidationRequest) {
        Log.v(TAG, "addNotificationToken")
        if(!this::userId.isInitialized) {
            // This occurs when the app has just been installed and
            // this method is called from inside PushNotificationService.
            // It will fail for sure for being then called once again
            // inside LoginActivity, perhaps successfully.
            Log.w(TAG, "Property userId was not yet initialized.")
            return
        }
        val response = try {
            trainRequest.user = userId
            api.trainModel(trainRequest)
        } catch (e: IOException) {
            e.message?.let { Log.e(TAG, it) }
            return
        } catch (e: HttpException) {
            e.message?.let { Log.e(TAG, it) }
            return
        }

        if(response.isSuccessful) {
            Log.i(TAG, "Model trained succesfully")
        } else {
            Log.e(TAG, response.errorBody().toString())
        }
    }

    suspend fun validityPlace(validityRequest: ValidationRequest) {
        Log.v(TAG, "addNotificationToken")
        if(!this::userId.isInitialized) {
            // This occurs when the app has just been installed and
            // this method is called from inside PushNotificationService.
            // It will fail for sure for being then called once again
            // inside LoginActivity, perhaps successfully.
            Log.w(TAG, "Property userId was not yet initialized.")
            return
        }
        val response = try {
            validityRequest.user = userId
            api.validityPlace(validityRequest)
        } catch (e: IOException) {
            e.message?.let { Log.e(TAG, it) }
            return
        } catch (e: HttpException) {
            e.message?.let { Log.e(TAG, it) }
            return
        }

        if(response.isSuccessful) {
            Log.i(TAG, "Validity place succesfully")
        } else {
            Log.e(TAG, response.errorBody().toString())
        }
    }

    suspend fun recommendPlace(placeRequest: PlaceRequest) {
        Log.v(TAG, "addNotificationToken")
        if(!this::userId.isInitialized) {
            // This occurs when the app has just been installed and
            // this method is called from inside PushNotificationService.
            // It will fail for sure for being then called once again
            // inside LoginActivity, perhaps successfully.
            Log.w(TAG, "Property userId was not yet initialized.")
            return
        }
        val response = try {
            placeRequest.user = userId
            api.recommendPlace(placeRequest)
        } catch (e: IOException) {
            e.message?.let { Log.e(TAG, it) }
            return
        } catch (e: HttpException) {
            e.message?.let { Log.e(TAG, it) }
            return
        }

        if(response.isSuccessful) {
            Log.i(TAG, "Place recommendation succesfully")
        } else {
            Log.e(TAG, response.errorBody().toString())
        }
    }


}

