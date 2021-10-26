package it.unibo.socialplaces.api

sealed class ApiError(val msg: String) {
    data class Message(val message: String): ApiError(message)
    class Generic: ApiError("Impossible to retrieve the body of the error response.")
}
