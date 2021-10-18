package com.example.maptry.model.pointofinterests

data class AddPointOfInterestPoi(
    val address: String,
    val type: String,
    val latitude: Double,
    val longitude: Double,
    val name: String,
    val phoneNumber: String,
    val visibility: String,
    val url: String,
) {
    constructor(poi: PointOfInterest): this(poi.address, poi.type, poi.latitude, poi.longitude, poi.name, poi.phoneNumber, poi.visibility, poi.url)
}
