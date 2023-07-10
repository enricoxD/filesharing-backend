package de.enricoe.api.requests

import kotlinx.serialization.Serializable

@Serializable
data class GetUploadRequest(
        val author: String,
        val id: String,
        val password: String?
)