package com.example.maptry.model.liveevents

data class LiveEvent(
    val id: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val name: String,
    val owner: String,
    val expirationDate: Long
)
