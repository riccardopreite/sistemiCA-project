package com.example.maptry.domain

import android.util.Log
import com.example.maptry.activity.MapsActivity
import com.example.maptry.api.RetrofitInstances
import com.example.maptry.model.pointofinterests.AddPointOfInterest
import com.example.maptry.model.pointofinterests.PointOfInterest
import com.example.maptry.model.pointofinterests.RemovePointOfInterest
import retrofit2.HttpException
import java.io.IOException

object PointsOfInterest {
    private const val TAG = "domain.PointsOfInterest"

    val api by lazy {
        RetrofitInstances.pointOfInterestsApi
    }

    lateinit var userId: String

    suspend fun getPointsOfInterest(user: String = ""): List<PointOfInterest> {
        val response = try {
            api.getPointsOfInterest(userId, user)
        } catch (e: IOException) {
            e?.message?.let {
                Log.e(TAG, it)
                Log.e(TAG, "Returning empty points of interest list.")
            }
            return emptyList()
        } catch (e: HttpException) {
            e?.message?.let {
                Log.e(TAG, it)
                Log.e(TAG, "Returning empty points of interest list.")
            }
            return emptyList()
        }

        if(response.isSuccessful && response.body() != null) {
            Log.i(TAG, "Found ${response.body()!!.size} points of interest of user $user.")
            return response.body()!!
        }

        return emptyList()
    }

    suspend fun addPointOfInterest(addPointOfInterest: AddPointOfInterest): String {
        val response = try {
            api.addPointOfInterest(addPointOfInterest)
        } catch (e: IOException) {
            e?.message?.let {
                Log.e(TAG, it)
                Log.e(TAG, "Returning empty point of interest id.")
            }
            return ""
        } catch (e: HttpException) {
            e?.message?.let {
                Log.e(TAG, it)
                Log.e(TAG, "Returning empty point of interest id.")
            }
            return ""
        }

        if(response.isSuccessful && response.body() != null) {
            Log.i(TAG, "Point of interest successfully added.")
            return response.body()!!
        }

        return ""
    }

    suspend fun removePointOfInterest(pointOfInterest: PointOfInterest) {
        val response = try {
            api.removePointOfInterest(RemovePointOfInterest(userId, pointOfInterest.markId))
        } catch (e: IOException) {
            e?.message?.let { Log.e(TAG, it) }
            return
        } catch (e: HttpException) {
            e?.message?.let { Log.e(TAG, it) }
            return
        }

        if(response.isSuccessful) {
            Log.i(TAG, "Point of interest successfully removed.")
        }
    }
}