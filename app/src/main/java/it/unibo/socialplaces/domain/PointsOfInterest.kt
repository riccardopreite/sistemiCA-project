package it.unibo.socialplaces.domain

import android.util.Log
import it.unibo.socialplaces.api.ApiError
import it.unibo.socialplaces.api.RetrofitInstances
import it.unibo.socialplaces.model.pointofinterests.AddPointOfInterest
import it.unibo.socialplaces.model.pointofinterests.PointOfInterest
import it.unibo.socialplaces.model.pointofinterests.RemovePointOfInterest
import okhttp3.ResponseBody
import retrofit2.HttpException
import java.io.IOException

object PointsOfInterest {
    private const val TAG = "domain.PointsOfInterest"

    private val api by lazy {
        RetrofitInstances.pointOfInterestsApi
    }

    private val handleApiError: (ResponseBody?) -> ApiError = RetrofitInstances::handleApiError

    private lateinit var userId: String

    private val pointsOfInterest: MutableList<PointOfInterest> = emptyList<PointOfInterest>().toMutableList()

    private lateinit var createMarker: (Double,Double,String,String,String,Boolean) -> Unit
    private lateinit var updateList: () -> Unit
    private var validCreateMarkerCallback: Boolean = false
    private var validUpdateListCallback: Boolean = false

    fun setUserId(user: String) {
        Log.v(TAG, "setUserId")
        userId = user
    }

    fun setCreateMarkerCallback(createMarker:(Double,Double,String,String,String,Boolean) -> Unit){
        this.createMarker = createMarker
        validCreateMarkerCallback = true
    }

    fun disableCallback() {
        validCreateMarkerCallback = false
        validUpdateListCallback = false
    }

    fun setUpdatePoiCallback(updatePoi:() -> Unit){
        this.updateList = updatePoi
        validUpdateListCallback = true
    }


    suspend fun getPointsOfInterest(user: String = "", forceSync: Boolean = false): List<PointOfInterest> {
        Log.v(TAG, "getPointsOfInterest")
        if((user == "" && !forceSync) || (user != "" && user == userId && !forceSync)) {
            return pointsOfInterest
        }

        val response = try {
            api.getPointsOfInterest(userId, user)
        } catch (e: IOException) {
            e.message?.let {
                Log.e(TAG, it)
                Log.e(TAG, "Returning empty points of interest list.")
            }
            return emptyList()
        } catch (e: HttpException) {
            e.message?.let {
                Log.e(TAG, it)
                Log.e(TAG, "Returning empty points of interest list.")
            }
            return emptyList()
        }

        if(response.isSuccessful && response.body() != null) {
            Log.i(TAG, "Found ${response.body()!!.size} points of interest of user $user.")
            return if(user != "" && user != userId) {
                // Other users' pois are not cached.
                response.body()!!
            } else {
                pointsOfInterest.clear()
                pointsOfInterest.addAll(response.body()!!)
                pointsOfInterest
            }
        } else {
            Log.e(TAG, handleApiError(response.errorBody()).toString())
        }

        return emptyList()
    }

    suspend fun addPointOfInterest(addPointOfInterest: AddPointOfInterest): String {
        Log.v(TAG, "addPointOfInterest")
        val response = try {
            if(addPointOfInterest.user == "") {
                api.addPointOfInterest(addPointOfInterest.copy(user = userId))
            } else {
                api.addPointOfInterest(addPointOfInterest)
            }
        } catch (e: IOException) {
            e.message?.let {
                Log.e(TAG, it)
                Log.e(TAG, "Returning empty point of interest id.")
            }
            return ""
        } catch (e: HttpException) {
            e.message?.let {
                Log.e(TAG, it)
                Log.e(TAG, "Returning empty point of interest id.")
            }
            return ""
        }

        if(response.isSuccessful && response.body() != null) {
            Log.i(TAG, "Point of interest successfully added.")
            val addedPointOfInterest = response.body()!!
            Log.i(TAG, addedPointOfInterest.markId)
            val currentPOI = PointOfInterest(
                addedPointOfInterest.markId,
                addPointOfInterest.poi.address,
                addPointOfInterest.poi.type,
                addPointOfInterest.poi.latitude,
                addPointOfInterest.poi.longitude,
                addPointOfInterest.poi.name,
                addPointOfInterest.poi.phoneNumber,
                addPointOfInterest.poi.visibility,
                addPointOfInterest.poi.url
            )
            addPointOfInterestLocally(currentPOI)
            if(validCreateMarkerCallback) {
                createMarker(
                    addPointOfInterest.poi.latitude,
                    addPointOfInterest.poi.longitude,
                    addPointOfInterest.poi.name,
                    addPointOfInterest.poi.address,
                    addedPointOfInterest.markId,
                    false
                )
            }
            if(validUpdateListCallback) {
                updateList()
            }

            return addedPointOfInterest.markId
        } else {
            Log.e(TAG, handleApiError(response.errorBody()).toString())
        }

        return ""
    }

    suspend fun removePointOfInterest(pointOfInterest: PointOfInterest) {
        Log.v(TAG, "removePointOfInterest")
        val response = try {
            api.removePointOfInterest(RemovePointOfInterest(userId, pointOfInterest.markId))
        } catch (e: IOException) {
            e.message?.let { Log.e(TAG, it) }
            return
        } catch (e: HttpException) {
            e.message?.let { Log.e(TAG, it) }
            return
        }

        if(response.isSuccessful) {
            Log.i(TAG, "Point of interest successfully removed.")
        } else {
            Log.e(TAG, handleApiError(response.errorBody()).toString())
        }
    }

    fun removePointOfInterestLocally(pointOfInterest: PointOfInterest) {
        Log.v(TAG, "removePointOfInterestLocally")

        pointsOfInterest.remove(pointOfInterest)
        pointsOfInterest.sortBy { it.name }
    }

    fun addPointOfInterestLocally(pointOfInterest: PointOfInterest) {
        Log.v(TAG, "addPointOfInterestLocally")

        pointsOfInterest.add(pointOfInterest)
        pointsOfInterest.sortBy { it.name }
    }
}