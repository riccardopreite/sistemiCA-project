package com.example.maptry.model.pointofinterests

data class AddPointOfInterest(
    val address: String,
    val type: String,
    val latitude: Double,
    val longitude: Double,
    val name: String,
    val phoneNumber: String,
    val visibility: String,
    val url: String,
)
