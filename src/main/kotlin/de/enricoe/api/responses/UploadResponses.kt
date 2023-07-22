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

@Serializable
class UploadListEntry(
    val id: String,
    val author: String,
    val authorName: String,
    val title: String,
    val uploadedAt: LocalDateTime,
    val size: Long,
    val filesAmount: Int
)