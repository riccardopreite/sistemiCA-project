package com.example.maptry.domain

import android.util.Log
import com.example.maptry.api.ApiError
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

    private val pointsOfInterest: MutableList<PointOfInterest> = emptyList<PointOfInterest>().toMutableList()

    suspend fun getPointsOfInterest(user: String = "", forceSync: Boolean = false): List<PointOfInterest> {
        if(!forceSync) {
            return pointsOfInterest
        }

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
            pointsOfInterest.addAll(response.body()!!)
            return pointsOfInterest
        } else {
            Log.e(TAG, (response.errorBody() as ApiError).message)
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
        } else {
            Log.e(TAG, (response.errorBody() as ApiError).message)
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
        } else {
            Log.e(TAG, (response.errorBody() as ApiError).message)
        }
    }
}