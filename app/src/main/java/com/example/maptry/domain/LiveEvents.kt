package com.example.maptry.domain

import android.util.Log
import com.example.maptry.api.ApiError
import com.example.maptry.api.RetrofitInstances
import com.example.maptry.model.liveevents.AddLiveEvent
import com.example.maptry.model.liveevents.LiveEvent
import retrofit2.HttpException
import java.io.IOException

object LiveEvents {
    private const val TAG = "domain.LiveEvents"

    val api by lazy {
        RetrofitInstances.liveEventsApi
    }

    lateinit var userId: String

    suspend fun getLiveEvents(): List<LiveEvent> {
        val response = try {
            api.getLiveEvents(userId)
        } catch (e: IOException) {
            e?.message?.let {
                Log.e(TAG, it)
                Log.e(TAG, "Returning empty live events list.")
            }
            return emptyList()
        } catch (e: HttpException) {
            e?.message?.let {
                Log.e(TAG, it)
                Log.e(TAG, "Returning empty live events list.")
            }
            return emptyList()
        }

        if(response.isSuccessful && response.body() != null) {
            Log.i(TAG, "Found ${response.body()!!.size} live events.")
            return response.body()!!
        } else {
            Log.e(TAG, (response.errorBody() as ApiError).message)
        }

        return emptyList()
    }

    suspend fun addLiveEvent(addLiveEvent: AddLiveEvent) {
        val response = try {
            api.addLiveEvent(addLiveEvent)
        } catch (e: IOException) {
            e?.message?.let { Log.e(TAG, it) }
            return
        } catch (e: HttpException) {
            e?.message?.let { Log.e(TAG, it) }
            return
        }

        if(response.isSuccessful && response.body() != null) {
            Log.i(TAG, "Point of interest successfully added.")
        } else {
            Log.e(TAG, (response.errorBody() as ApiError).message)
        }
    }
}