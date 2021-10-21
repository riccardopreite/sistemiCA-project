package it.unibo.socialplaces.model.friends

data class RemoveFriendshipRequest(
    val receiver: String,
    val sender: String
)
