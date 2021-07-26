package com.example.maptry.dataclass


data class AddPointOfInterest(
    var user: String? = "",
    var poi: UserMarker? = UserMarker("","","","",0.0,0.0,"","","")
)
