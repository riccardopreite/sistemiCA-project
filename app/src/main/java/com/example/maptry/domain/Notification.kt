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
        Log.v(TAG, "removePointOfInterest")
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

