package de.enricoe.api.responses.analytics

import kotlinx.serialization.Serializable

@Serializable
data class PublicInformationResponse(
    val uploads: Int,
    val downloads: Int,
    val users: Long,
)