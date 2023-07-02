package de.enricoe.repository

import de.enricoe.database.MongoManager
import de.enricoe.models.FileUpload
import de.enricoe.models.Upload
import de.enricoe.models.User
import de.enricoe.security.Crypto
import de.enricoe.utils.FileHasher
import de.enricoe.utils.Response
import io.ktor.http.content.*
import kotlinx.datetime.*
import org.litote.kmongo.and
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.time.Duration.Companion.days

object FilesRepository {

    enum class DeleteIn {
        ONE_DAY,
        ONE_WEEK,
        TWO_WEEKS,
        ONE_MONTH,
        THREE_MONTH;

        fun toTimestamp(uploadTime: LocalDateTime): LocalDateTime {
            return uploadTime.toInstant(TimeZone.currentSystemDefault()).plus(
                when (this) {
                    ONE_DAY -> 2.days
                    ONE_WEEK -> 7.days
                    TWO_WEEKS -> 14.days
                    ONE_MONTH -> 30.days
                    THREE_MONTH -> 90.days
                }
            ).toLocalDateTime(TimeZone.currentSystemDefault())
        }
    }

    suspend fun uploadFiles(author: String, multiPartData: MultiPartData): Response<Any> {
        val uploadTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        var title: String = ""
        var password: String = ""
        var deleteIn: DeleteIn = DeleteIn.valueOf("ONE_WEEK")
        val files = arrayListOf<FileUpload>()

        runCatching {
            multiPartData.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        val name = part.name
                        val value = part.value
                        println("$name: $value")

                        when (name) {
                            "title" -> title = value.trim()
                            "password" -> password = Crypto.hashPassword(value)
                            "deleteIn" -> deleteIn = DeleteIn.valueOf(value)
                        }
                    }

                    is PartData.FileItem -> {
                        // Handle uploaded file
                        val fileName = part.originalFileName ?: "unknown"
                        val stream = part.streamProvider()
                        val byteArray = stream.readBytes()
                        stream.close()

                        val hash = FileHasher.hash(ByteArrayInputStream(byteArray.clone()))
                        val directory = File("/uploads/$author/").also { if (!it.exists()) it.mkdirs() }
                        val targetFile = File(directory, hash)
                        if (targetFile.exists()) {
                            return@forEachPart
                        }
                        Files.copy(
                            ByteArrayInputStream(byteArray.clone()),
                            targetFile.toPath(),
                            StandardCopyOption.REPLACE_EXISTING
                        )
                        files.add(FileUpload(fileName, hash))
                    }

                    else -> {}
                }
                part.dispose()
            }

            val upload = Upload(author, title, password.takeIf { it.trim().isNotBlank() }, uploadTime, files.toTypedArray(), deleteIn.toTimestamp(uploadTime))
            MongoManager.uploads.insertOne(upload)
            println("$author/${upload.contentHash}")
            return Response.Success(upload, "$author/${upload.contentHash}")
        }.onFailure {
            return Response.Error(message = "Upload failed")
        }
        return Response.Error(message = "?")
    }

    suspend fun getUpload(user: User?, password: String?, author: String, hash: String): Response<Any> {
        val upload = MongoManager.uploads.findOne(and(Upload::author eq author, Upload::contentHash eq hash))
            ?: return Response.Error(message = "Requested Upload not found")
        if (user?.id != upload.author) {
            val uploadPw = upload.password
            if (uploadPw != null) {
                if (password == null || !Crypto.verifyPassword(password, uploadPw).verified)
                    return Response.Error(message = "Invalid password")
            }
        }
        return Response.Success(upload)
    }
}