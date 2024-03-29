package it.unibo.socialplaces.domain

import android.util.Log
import it.unibo.socialplaces.api.ApiError
import it.unibo.socialplaces.api.ApiConnectors
import it.unibo.socialplaces.model.notification.NotificationToken
import it.unibo.socialplaces.model.notification.PublicKey
import okhttp3.ResponseBody
import retrofit2.HttpException
import java.io.IOException

object UserData {
    private val TAG = UserData::class.qualifiedName!!

    private val api by lazy {
        ApiConnectors.userDataApi
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
     * Calls POST /user-data/notification-token in the SocialPlaces API.
     * @see it.unibo.socialplaces.api.UserDataApi
     * @param token the push notification token
     */
    suspend fun addNotificationToken(token: String) {
        Log.v(TAG, "addNotificationToken")
        if(!this::userId.isInitialized) {
            /*
             * This occurs when the app has just been installed and
             * this method is called from inside PushNotificationService.
             * It will fail for sure for being then called once again
             * inside LoginActivity, perhaps successfully.
             * One may notice that the other classes inside this same package
             * do not make this initial check: it is not necessary since they'll be initialized
             * for sure.
             */
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
            Log.i(TAG, "Push notification service token updated successfully.")
        } else {
            Log.e(TAG, handleApiError(response.errorBody()).toString())
        }
    }

    /**
     * Calls POST /user-data/public-key in the SocialPlaces API.
     * @see it.unibo.socialplaces.api.UserDataApi
     * @param token the public key
     */
    suspend fun addPublicKey(publicKey: String) {
        Log.v(TAG, "addPublicKey")
        if(!this::userId.isInitialized) {
            /*
             * This occurs when the app has just been installed and
             * this method is called from inside PushNotificationService.
             * It will fail for sure for being then called once again
             * inside LoginActivity, perhaps successfully.
             * One may notice that the other classes inside this same package
             * do not make this initial check: it is not necessary since they'll be initialized
             * for sure.
             */
            Log.w(TAG, "Property userId was not yet initialized.")
            return
        }
        val response = try {
            api.addPublicKey(PublicKey(userId, publicKey))
        } catch (e: IOException) {
            e.message?.let { Log.e(TAG, it) }
            return
        } catch (e: HttpException) {
            e.message?.let { Log.e(TAG, it) }
            return
        }

        if(response.isSuccessful) {
            Log.i(TAG, "Public key successfully sent.")
        } else {
            Log.e(TAG, handleApiError(response.errorBody()).toString())
        }
    }
}

