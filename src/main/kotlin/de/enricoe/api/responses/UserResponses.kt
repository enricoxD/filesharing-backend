package de.enricoe.api.responses

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import java.nio.file.Path

@Serializable
data class UserResponse(
    val id: String,
    var username: String,
    var email: String,
    val createdAt: LocalDateTime,
    var lastSeen: LocalDateTime,
    var avatar: Path? = null,
    var authToken: String? = null,
    var emailVerified: Boolean = false,
)

@Serializable
data class ForeignUserResponse(
    val id: String,
    var username: String,
    val createdAt: LocalDateTime,
    var lastSeen: LocalDateTime,
    var avatar: Path? = null,
)