package de.enricoe.api.requests

import de.enricoe.models.FileUpload
import kotlinx.serialization.Serializable

@Serializable
data class FileDownloadRequest(
        val author: String,
        val id: String,
        val fileUpload: FileUpload,
        val password: String?
)