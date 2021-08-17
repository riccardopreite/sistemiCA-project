package com.example.maptry.model.liveevents

data class LiveEvent(
    val id: String,
    val address: String,
    val name: String,
    val owner: String,
    val expirationDate: Int
)
