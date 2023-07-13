package de.enricoe.api.responses

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class AuthorInformationResponse(
        val name: String,
        val lastSeen: LocalDateTime?
)