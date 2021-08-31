package com.example.maptry.domain

import android.util.Log
import com.example.maptry.api.ApiError
import com.example.maptry.api.RetrofitInstances
import com.example.maptry.model.friends.AddFriendshipConfirmation
import com.example.maptry.model.friends.AddFriendshipRequest
import com.example.maptry.model.friends.Friend
import com.example.maptry.model.friends.RemoveFriendshipRequest
import retrofit2.HttpException
import java.io.IOException

object Friends {
    private const val TAG = "domain.Friends"

    private val api by lazy {
        RetrofitInstances.friendsApi
    }

    private lateinit var userId: String

    private val friends: MutableList<Friend> = emptyList<Friend>().toMutableList()

    suspend fun getFriends(forceSync: Boolean = false): List<Friend> {
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
            friends.addAll(response.body()!!)
            return friends
        } else {
            Log.e(TAG, (response.errorBody() as ApiError).message)
        }

        return emptyList()
    }

    suspend fun addFriend(friendUsername: String) {
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
            Log.e(TAG, (response.errorBody() as ApiError).message)
        }
    }

    suspend fun confirmFriend(otherUserUsername: String) {
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
            Log.e(TAG, (response.errorBody() as ApiError).message)
        }
    }

    suspend fun removeFriend(friendUsername: String) {
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
            Log.e(TAG, (response.errorBody() as ApiError).message)
        }
    }
}