package it.unibo.socialplaces.model.pointofinterests

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PointOfInterest(
    val markId: String,
    val address: String,
    val type: String,
    val latitude: Double,
    val longitude: Double,
    val name: String,
    val phoneNumber: String,
    val visibility: String,
    val url: String
): Parcelable
