package it.unibo.socialplaces.domain

import android.util.Log
import it.unibo.socialplaces.api.ApiError
import it.unibo.socialplaces.api.ApiConnectors
import it.unibo.socialplaces.model.pointofinterests.AddPointOfInterest
import it.unibo.socialplaces.model.pointofinterests.PointOfInterest
import it.unibo.socialplaces.model.pointofinterests.RemovePointOfInterest
import okhttp3.ResponseBody
import retrofit2.HttpException
import java.io.IOException

object PointsOfInterest {
    private val TAG = PointsOfInterest::class.qualifiedName!!

    private val api by lazy {
        ApiConnectors.pointsOfInterestApi
    }

    private val handleApiError: (ResponseBody?) -> ApiError = ApiConnectors::handleApiError

    private lateinit var userId: String

    private val pointsOfInterest: MutableList<PointOfInterest> = emptyList<PointOfInterest>().toMutableList()

    private lateinit var createMarker: (Double,Double,String,String,String,String) -> Unit
    private lateinit var updateList: () -> Unit
    private var validCreateMarkerCallback: Boolean = false
    private var validUpdateListCallback: Boolean = false

    /**
     * Sets the default user (logged in user) for API calls.
     * @param user the logged in user.
     */
    fun setUserId(user: String) {
        Log.v(TAG, "setUserId")
        userId = user
    }

    /**
     * Sets a callback to be invoked every time [addPointOfInterest] is called.
     * This callback will stop being invoked once [disableCallbacks] is invoked.
     */
    fun setCreateMarkerCallback(createMarker: (Double,Double,String,String,String,String) -> Unit){
        this.createMarker = createMarker
        validCreateMarkerCallback = true
    }

    /**
     * Sets a callback to be invoked every time [addPointOfInterest] is called.
     * This callback will stop being invoked once [disableCallbacks] is invoked.
     */
    fun setUpdatePoiCallback(updatePoi:() -> Unit){
        this.updateList = updatePoi
        validUpdateListCallback = true
    }

    /**
     * Disables the invocation of the callbacks set via [setCreateMarkerCallback] and
     * [setUpdatePoiCallback].
     */
    fun disableCallbacks() {
        validCreateMarkerCallback = false
        validUpdateListCallback = false
    }


    /**
     * Calls GET /points-of-interest in the SocialPlaces API.
     * @see [it.unibo.socialplaces.api.PointsOfInterestApi.getPointsOfInterest]
     * @param forceSync when `true` actually calls the remote APIs, otherwise retrieves the cached data
     * (which could be empty).
     * @param user when not empty retrieves the points of interest of user with this username (without caching them),
     * otherwise the user's points of interest.
     * @return the list of live events of the logged user and their friends.
     */
    suspend fun getPointsOfInterest(user: String = "", forceSync: Boolean = false): List<PointOfInterest> {
        Log.v(TAG, "getPointsOfInterest")
        if((user == "" && !forceSync) || (user != "" && user == userId && !forceSync)) {
            return pointsOfInterest
        }

        val response = try {
            api.getPointsOfInterest(userId, user)
        } catch (e: IOException) {
            e.message?.let { Log.e(TAG, "$it\nReturning empty points of interest list.") }
            return emptyList()
        } catch (e: HttpException) {
            e.message?.let { Log.e(TAG, "$it\nReturning empty points of interest list.") }
            return emptyList()
        }

        val body = response.body()
        if(response.isSuccessful && body != null) {
            Log.i(TAG, "Found ${body.size} points of interest of user ${if(user.isNotEmpty()) user else userId}.")
            return if(user != "" && user != userId) {
                // Other users' points of interest are not cached.
                body
            } else {
                pointsOfInterest.clear()
                pointsOfInterest.addAll(body)
                pointsOfInterest
            }
        } else {
            Log.e(TAG, handleApiError(response.errorBody()).toString())
        }

        return emptyList()
    }

    /**
     * Calls POST /points-of-interest/add in the SocialPlaces API.
     * @see [it.unibo.socialplaces.api.PointsOfInterestApi.addPointOfInterest]
     * @param addPointOfInterest the data for the point of interest to add.
     * @return the id of the added point of interest.
     */
    suspend fun addPointOfInterest(addPointOfInterest: AddPointOfInterest): String {
        Log.v(TAG, "addPointOfInterest")
        val response = try {
            if(addPointOfInterest.user == "") {
                api.addPointOfInterest(addPointOfInterest.copy(user = userId))
            } else {
                api.addPointOfInterest(addPointOfInterest)
            }
        } catch (e: IOException) {
            e.message?.let { Log.e(TAG, "$it\nReturning empty point of interest id.") }
            return ""
        } catch (e: HttpException) {
            e.message?.let { Log.e(TAG, "$it\nReturning empty point of interest id.") }
            return ""
        }

        val addedPointOfInterest = response.body()
        if(response.isSuccessful && addedPointOfInterest != null) {
            Log.i(TAG, "Point of interest successfully added.")
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
                    addPointOfInterest.poi.type
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

    /**
     * Calls DELETE /points-of-interest/remove in the SocialPlaces API.
     * @see [it.unibo.socialplaces.api.PointsOfInterestApi.removePointOfInterest]
     * @param pointOfInterest the point of interest to remove.
     */
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

    /**
     * Removes a point of interest locally and sorts the list of points of interest.
     * @param pointOfInterest point of interest to remove (locally).
     */
    fun removePointOfInterestLocally(pointOfInterest: PointOfInterest) {
        Log.v(TAG, "removePointOfInterestLocally")

        pointsOfInterest.remove(pointOfInterest)
        pointsOfInterest.sortBy { it.name }
    }

    /**
     * Adds a point of interest locally and sorts the list of points of interest.
     * @param pointOfInterest new point of interest to add (locally).
     */
    fun addPointOfInterestLocally(pointOfInterest: PointOfInterest) {
        Log.v(TAG, "addPointOfInterestLocally")

        pointsOfInterest.add(pointOfInterest)
        pointsOfInterest.sortBy { it.name }
    }
}