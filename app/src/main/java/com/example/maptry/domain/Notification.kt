package com.example.maptry.domain

import android.util.Log
import com.example.maptry.api.RetrofitInstances
import com.example.maptry.model.notification.NotificationToken
import com.example.maptry.model.pointofinterests.PointOfInterest
import com.example.maptry.model.pointofinterests.RemovePointOfInterest
import retrofit2.HttpException
import java.io.IOException

object Notification {
    private const val TAG = "domain.Notification"

    private val api by lazy {
        RetrofitInstances.notificationApi
    }

    private lateinit var userId: String

    fun setUserId(user: String) {
        Log.v(TAG, "setUserId")
        userId = user
    }

    suspend fun addNotificationToken(token: String) {
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
            api.addNotificationToken(NotificationToken(userId, token))
        } catch (e: IOException) {
            e.message?.let { Log.e(TAG, it) }
            return
        } catch (e: HttpException) {
            e.message?.let { Log.e(TAG, it) }
            return
        }

        if(response.isSuccessful) {
            Log.i(TAG, "Updated Token succesfully")
        } else {
            Log.e(TAG, response.errorBody().toString())
        }
    }


}

