package de.enricoe.api.requests

import kotlinx.serialization.Serializable

@Serializable
data class GetUserRequest(
    val id: String
)