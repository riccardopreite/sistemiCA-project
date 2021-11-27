package it.unibo.socialplaces.api

class ApiError(private val message: String = "Impossible to retrieve the body of the error response.") {
    override fun toString(): String = message
}
