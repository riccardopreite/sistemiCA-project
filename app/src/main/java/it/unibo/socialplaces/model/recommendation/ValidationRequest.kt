package it.unibo.socialplaces.model.recommendation

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ValidationRequest(
    // Only parameter which has the default value since the view cannot know in advance the username.
    val user: String = "",
    val latitude: Double,
    val longitude: Double,
    val human_activity: String,
    val seconds_in_day: Int,
    val week_day: Int,
    val place_category: String
): Parcelable
