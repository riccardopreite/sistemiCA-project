package it.unibo.socialplaces.domain

import android.util.Log
import it.unibo.socialplaces.api.ApiError
import it.unibo.socialplaces.api.ApiConnectors
import it.unibo.socialplaces.exception.FriendshipConfirmationNotSent
import it.unibo.socialplaces.exception.FriendshipDenialNotSent
import it.unibo.socialplaces.exception.FriendshipRemovalNotSent
import it.unibo.socialplaces.exception.FriendshipRequestNotSent
import it.unibo.socialplaces.model.friends.*
import okhttp3.ResponseBody
import retrofit2.HttpException
import java.io.IOException

object Friends {
    private val TAG = Friends::class.qualifiedName!!

    private val api by lazy {
        ApiConnectors.friendsApi
    }

    private val handleApiError: (ResponseBody?) -> ApiError = ApiConnectors::handleApiError

    private lateinit var userId: String

    private val friends: MutableList<Friend> = emptyList<Friend>().toMutableList()

    /**
     * Sets the default user (logged in user) for API calls.
     * @param user the logged in user.
     */
    fun setUserId(user: String) {
        Log.v(TAG, "setUserId")
        userId = user
    }

    /**
     * Calls GET /friends in the SocialPlaces API.
     * @see [it.unibo.socialplaces.api.FriendsApi.getFriends]
     * @param forceSync when `true` actually calls the remote APIs, otherwise retrieves the cached data
     * (which could be empty).
     * @return the list of friends of the logged user.
     */
    suspend fun getFriends(forceSync: Boolean = false): List<Friend> {
        Log.v(TAG, "getFriends")
        if(!forceSync) {
            return friends
        }

        val response = try {
            api.getFriends(userId)
        } catch (e: IOException) {
            e.message?.let { Log.e(TAG, "$it\nIOException - Returning empty friends list.") }
            return emptyList()
        } catch (e: HttpException) {
            e.message?.let { Log.e(TAG, "$it\nHttpException - Returning empty friends list.") }
            return emptyList()
        }

        if(response.isSuccessful && response.body() != null) {
            Log.i(TAG, "Found ${response.body()!!.size} friends.")
            friends.clear()
            friends.addAll(response.body()!!)
            return friends
        } else {
            Log.e(TAG, handleApiError(response.errorBody()).toString())
        }

        return emptyList()
    }

    /**
     * Calls POST /friends/add in the SocialPlaces API.
     * @see [it.unibo.socialplaces.api.FriendsApi.addFriend]
     * @param friendUsername the username of the friend to add.
     */
    suspend fun addFriend(friendUsername: String) {
        Log.v(TAG, "addFriend")
        val response = try {
            api.addFriend(AddFriendshipRequest(friendUsername, userId))
        } catch (e: IOException) {
            e.message?.let { Log.e(TAG, it) }
            return
        } catch (e: HttpException) {
            e.message?.let { Log.e(TAG, it) }
            return
        }

        if(response.isSuccessful) {
            Log.i(TAG, "Friend request to $friendUsername successfully sent.")
        } else {
            Log.d(TAG, response.code().toString())
            // TODO Perhaps throw an exception? https://github.com/riccardopreite/sistemiCA-project/issues/11
            val error = handleApiError(response.errorBody())
            Log.e(TAG, error.toString())
            throw FriendshipRequestNotSent(error.toString())
        }
    }

    /**
     * Calls POST /friends/confirm in the SocialPlaces API.
     * @see it.unibo.socialplaces.api.FriendsApi.confirmFriend
     * @param otherUserUsername the username of the friend that sent the friendship request.
     */
    suspend fun confirmFriend(otherUserUsername: String) {
        Log.v(TAG, "confirmFriend")
        val response = try {
           api.confirmFriend(AddFriendshipConfirmation(userId, otherUserUsername))
        } catch (e: IOException) {
            e.message?.let { Log.e(TAG, it) }
            return
        } catch (e: HttpException) {
            e.message?.let { Log.e(TAG, it) }
            return
        }

        if(response.isSuccessful) {
            Log.i(TAG, "Friend request coming from $otherUserUsername successfully confirmed.")
        } else {
            // TODO Perhaps throw an exception? https://github.com/riccardopreite/sistemiCA-project/issues/12
            val error = handleApiError(response.errorBody())
            Log.e(TAG, error.toString())
            throw FriendshipConfirmationNotSent(error.toString())
        }
    }

    /**
     * Calls POST /friends/deny in the SocialPlaces API.
     * @see it.unibo.socialplaces.api.FriendsApi.denyFriend
     * @param senderOfFriendshipRequest the username of the friend that sent the friendship request.
     */
    suspend fun denyFriend(senderOfFriendshipRequest: String) {
        Log.v(TAG, "denyFriend")
        val response = try {
            api.denyFriend(AddFriendshipDenial(userId, senderOfFriendshipRequest))
        } catch (e: IOException) {
            e.message?.let { Log.e(TAG, it) }
            return
        } catch (e: HttpException) {
            e.message?.let { Log.e(TAG, it) }
            return
        }

        if(response.isSuccessful) {
            Log.i(TAG, "Friend request coming from $senderOfFriendshipRequest successfully denied.")
        } else {
            // TODO Perhaps throw an exception? https://github.com/riccardopreite/sistemiCA-project/issues/13
            val error = handleApiError(response.errorBody())
            Log.e(TAG, error.toString())
            throw FriendshipDenialNotSent(error.toString())
        }
    }

    /**
     * Calls DELETE /friends/remove in the SocialPlaces API.
     * @see it.unibo.socialplaces.api.FriendsApi.removeFriend
     * @param friendUsername the username of the friend to remove.
     */
    suspend fun removeFriend(friendUsername: String) {
        Log.v(TAG, "removeFriend")
        val response = try {
            api.removeFriend(RemoveFriendshipRequest(friendUsername, userId))
        } catch (e: IOException) {
            e.message?.let { Log.e(TAG, it) }
            return
        } catch (e: HttpException) {
            e.message?.let { Log.e(TAG, it) }
            return
        }

        if(response.isSuccessful) {
            Log.i(TAG, "Friend with username $friendUsername successfully removed.")
        } else {
            // TODO Perhaps throw an exception? https://github.com/riccardopreite/sistemiCA-project/issues/14
            val error = handleApiError(response.errorBody())
            Log.e(TAG, error.toString())
            throw FriendshipRemovalNotSent(error.toString())
        }
    }

    fun removeFriendLocally(friend: Friend) {
        Log.v(TAG, "removeFriendLocally")

        friends.remove(friend)
    }

    fun addFriendLocally(friend: Friend) {
        Log.v(TAG, "addFriendLocally")

        friends.add(friend)
    }
}