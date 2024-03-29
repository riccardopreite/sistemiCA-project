package it.unibo.socialplaces.config

import it.unibo.socialplaces.domain.*

object Api {
    const val hostname: String = "192.168.1.3"
    const val port: String = "3000"

    fun setUserId(userId: String?) {
        userId?.let {
            UserData.setUserId(it)
            Recommendation.setUserId(it)
            Friends.setUserId(it)
            LiveEvents.setUserId(it)
            PointsOfInterest.setUserId(it)
        }
    }
}