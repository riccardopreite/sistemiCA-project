package it.unibo.socialplaces.model.recommendation

data class PlaceRequest (
    val user: String,
    val latitude: Double,
    val longitude: Double,
    val human_activity: String,
    val seconds_in_day: Int,
    val week_day: Int
)