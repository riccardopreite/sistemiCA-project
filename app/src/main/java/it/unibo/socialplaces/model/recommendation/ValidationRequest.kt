package it.unibo.socialplaces.model.recommendation

data class ValidationRequest (
    var user: String = "",
    var latitude: Double =0.0,
    var longitude: Double = 0.0,
    var human_activity: String = "",
    var seconds_in_day: Int = 0,
    var week_day: Int = 0,
    var place_category: String = ""
)
