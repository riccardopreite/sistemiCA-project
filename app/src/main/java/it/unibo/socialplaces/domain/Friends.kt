package it.unibo.socialplaces.domain

import android.util.Log
import it.unibo.socialplaces.api.ApiError
import it.unibo.socialplaces.api.RetrofitInstances
import it.unibo.socialplaces.model.friends.*
import it.unibo.socialplaces.model.pointofinterests.PointOfInterest
import okhttp3.ResponseBody
import retrofit2.HttpException
import java.io.IOException

object Friends {
    private val TAG = Friends::class.qualifiedName

    private val api by lazy {
        RetrofitInstances.friendsApi
    }

    private val handleApiError: (ResponseBody?) -> ApiError = RetrofitInstances::handleApiError

    private lateinit var userId: String

    private val friends: MutableList<Friend> = emptyList<Friend>().toMutableList()

    fun setUserId(user: String) {
        Log.v(TAG, "setUserId")
        userId = user
    }

    suspend fun getFriends(forceSync: Boolean = false): List<Friend> {
        Log.v(TAG, "getFriends")
        if(!forceSync) {
            return friends
        }

        val response = try {
            api.getFriends(userId)
        } catch (e: IOException) {
            e.message?.let {
                Log.e(TAG, it)
                Log.e(TAG, "Returning empty friends list.")
            }
            return emptyList()
        } catch (e: HttpException) {
            e.message?.let {
                Log.e(TAG, it)
                Log.e(TAG, "Returning empty friends list.")
            }
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
            Log.e(TAG, handleApiError(response.errorBody()).toString())
        }
    }

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
            Log.e(TAG, handleApiError(response.errorBody()).toString())
        }
    }

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
            Log.e(TAG, handleApiError(response.errorBody()).toString())
        }
    }

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
            Log.e(TAG, handleApiError(response.errorBody()).toString())
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