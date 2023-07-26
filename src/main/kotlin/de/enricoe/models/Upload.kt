package de.enricoe.models

import de.enricoe.api.responses.UploadResponse
import de.enricoe.api.responses.UploadListEntry
import de.enricoe.database.MongoManager
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import java.io.File

@Serializable
class Upload(
    val author: String,
    var title: String,
    var password: String?,
    val uploadedAt: LocalDateTime,
    var files: Array<FileUpload>,
    var deleteAt: LocalDateTime,
    val sharedWith: MutableList<String> = mutableListOf()
) {
    @SerialName("_id")
    val id = buildString {
        append(uploadedAt.year)
        append(uploadedAt.monthNumber.toString().padStart(2, '0'))
        append(uploadedAt.dayOfMonth.toString().padStart(2, '0'))
        val minutes = uploadedAt.minute + uploadedAt.hour * 60
        val seconds = uploadedAt.second + minutes * 60
        append(seconds.toString().padStart(5, '0'))
        append(files.contentHashCode())
    }

    fun asResponse() = UploadResponse(id, author, title, uploadedAt, files)

    suspend fun asUploadListEntry(): UploadListEntry {
        val authorName = MongoManager.users.findOne(User::id eq author)?.name!!
        return UploadListEntry(id, author, authorName, title, uploadedAt, files.sumOf { it.size }, files.size)
    }
}

@Serializable
class FileUpload(
    @SerialName("_id") val id: String,
    val author: String,
    val name: String,
    val size: Long,
    var downloads: Int = 0
) {
    fun asFile() = File("/uploads/$author/$id")
}