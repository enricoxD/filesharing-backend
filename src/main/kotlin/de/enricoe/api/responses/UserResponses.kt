package de.enricoe.api.responses

import de.enricoe.models.Role
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import java.nio.file.Path

@Serializable
data class UserResponse(
    val id: String,
    val name: String,
    val email: String,
    val createdAt: LocalDateTime,
    val lastSeen: LocalDateTime,
    val emailVerified: Boolean = false,
    val role: Role,
)

@Serializable
data class ForeignUserResponse(
    val id: String,
    val name: String,
    val createdAt: LocalDateTime,
    val lastSeen: LocalDateTime,
    val role: Role,
)