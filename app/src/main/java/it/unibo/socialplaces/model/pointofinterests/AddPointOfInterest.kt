package it.unibo.socialplaces.model.pointofinterests

data class AddPointOfInterest(
    val poi: AddPointOfInterestPoi,
    val user: String = ""
)
