package de.enricoe.models

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
class Upload(
    val author: String,
    var title: String,
    var password: String?,
    val uploadedAt: LocalDateTime,
    var files: Array<FileUpload>,
    var deletedAt: LocalDateTime? = null,
) {
    val contentHash = buildString {
        append(uploadedAt.year)
        append(uploadedAt.monthNumber.toString().padStart(2, '0'))
        append(uploadedAt.dayOfMonth.toString().padStart(2, '0'))
        val minutes = uploadedAt.minute + uploadedAt.hour * 60
        val seconds = uploadedAt.second + minutes * 60
        append(seconds.toString().padStart(5, '0'))
        append(files.contentHashCode())
    }
}

@Serializable
class FileUpload(
    val fileName: String,
    val hash: String
)