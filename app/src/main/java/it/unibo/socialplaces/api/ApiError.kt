package it.unibo.socialplaces.api

sealed class ApiError(private val msg: String) {
    data class Message(val message: String): ApiError(message)
    class Generic: ApiError("Impossible to retrieve the body of the error response.")

    override fun toString(): String {
        return "[ApiError] $msg"
    }
}
