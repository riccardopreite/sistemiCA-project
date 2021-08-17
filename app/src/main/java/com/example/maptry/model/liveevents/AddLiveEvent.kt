package com.example.maptry.model.liveevents

data class AddLiveEvent(
    val expiresAfter: Int,
    val owner: String,
    val name: String,
    val address: String
)
