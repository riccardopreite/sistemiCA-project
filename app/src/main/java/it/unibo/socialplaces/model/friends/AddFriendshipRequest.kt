package it.unibo.socialplaces.model.friends

data class AddFriendshipRequest(
    val receiver: String,
    val sender: String
)
