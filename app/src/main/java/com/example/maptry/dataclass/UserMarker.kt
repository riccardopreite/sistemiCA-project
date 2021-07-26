package com.example.maptry.dataclass

data class UserMarker(
    var name: String? = "",
    var address: String? = "",
    var type: String? = "",
    var visibility: String? = "",
    var latitude: Double? = 0.0,
    var longitude: Double? = 0.0,
    var url: String? = "",
    var phoneNumber: String? = "",
    var markId: String? = ""
)
