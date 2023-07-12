package de.enricoe.api.responses

import de.enricoe.models.Role
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
    var emailVerified: Boolean = false,
    var role: Role
)

@Serializable
data class ForeignUserResponse(
    val id: String,
    var username: String,
    val createdAt: LocalDateTime,
    var lastSeen: LocalDateTime,
    var role: Role
)