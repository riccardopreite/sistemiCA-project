package it.unibo.socialplaces.model.friends

data class AddFriendshipConfirmation(
    val receiverOfTheFriendshipRequest: String,
    val senderOfTheFriendshipRequest: String
)
