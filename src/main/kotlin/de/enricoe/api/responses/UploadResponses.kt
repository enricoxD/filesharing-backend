package de.enricoe.api.responses

import de.enricoe.models.FileUpload
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
class UploadResponse(
    val id: String,
    val author: String,
    var title: String,
    val uploadedAt: LocalDateTime,
    var files: Array<FileUpload>,
)