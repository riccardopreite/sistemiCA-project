package it.unibo.socialplaces.model.friends

data class AddFriendshipDenial(
    val receiverOfTheFriendshipRequest: String,
    val senderOfTheFriendshipRequest: String
)
