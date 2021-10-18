package com.example.maptry.model.liveevents

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LiveEvent(
    val id: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val name: String,
    val owner: String,
    val expirationDate: Long
): Parcelable
