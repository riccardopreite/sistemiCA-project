package com.example.maptry.model.friends

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Friend(
    val id: String,
    val friendUsername: String
): Parcelable