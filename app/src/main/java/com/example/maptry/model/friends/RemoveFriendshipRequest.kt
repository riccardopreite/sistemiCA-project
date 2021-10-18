package com.example.maptry.model.friends

data class RemoveFriendshipRequest(
    val receiver: String,
    val sender: String
)
