package it.unibo.socialplaces.api

import it.unibo.socialplaces.model.friends.*
import retrofit2.Response
import retrofit2.http.*

interface FriendsApi {
    @GET("/friends")
    suspend fun getFriends(@Query("user") user: String): Response<List<Friend>>

    @POST("/friends/add")
    suspend fun addFriend(@Body addFriendshipRequest: AddFriendshipRequest): Response<Unit>

    @POST("/friends/confirm")
    suspend fun confirmFriend(@Body addFriendshipConfirmation: AddFriendshipConfirmation): Response<Unit>

    @POST("/friends/deny")
    suspend fun denyFriend(@Body addFriendshipDenial: AddFriendshipDenial): Response<Unit>

    @HTTP(method = "DELETE", path = "/friends/remove", hasBody = true   )
    suspend fun removeFriend(@Body removeFriendshipRequest: RemoveFriendshipRequest): Response<Unit>
}