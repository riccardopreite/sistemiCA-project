package com.example.maptry.dataclass

data class AddLiveEvent(
    var owner: String? = "",
    var poi: UserLive? = UserLive("","","","")
)
