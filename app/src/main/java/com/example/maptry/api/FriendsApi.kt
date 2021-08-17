package com.example.maptry.api

import com.example.maptry.model.friends.AddFriendshipConfirmation
import com.example.maptry.model.friends.AddFriendshipRequest
import com.example.maptry.model.friends.Friend
import com.example.maptry.model.friends.RemoveFriendshipRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST

interface FriendsApi {
    @GET("/friends")
    suspend fun getFriends(): Response<List<Friend>>

    @POST("/friends/add")
    suspend fun addFriend(@Body addFriendshipRequest: AddFriendshipRequest): Response<Unit>

    @POST("/friends/confirm")
    suspend fun confirmFriend(@Body addFriendshipConfirmation: AddFriendshipConfirmation): Response<Unit>

    @DELETE("/friends/remove")
    suspend fun removeFriend(@Body removeFriendshipRequest: RemoveFriendshipRequest): Response<Unit>
}