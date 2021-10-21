package it.unibo.socialplaces.model.liveevents

data class AddLiveEvent(
    val expiresAfter: Int,
    val owner: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
)
