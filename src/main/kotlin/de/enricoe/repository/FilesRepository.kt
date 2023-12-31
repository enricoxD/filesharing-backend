package de.enricoe.repository

import de.enricoe.api.responses.AuthorInformationResponse
import de.enricoe.database.MongoManager
import de.enricoe.models.FileUpload
import de.enricoe.models.Role
import de.enricoe.models.Upload
import de.enricoe.models.User
import de.enricoe.security.Crypto
import de.enricoe.utils.FileHasher
import de.enricoe.utils.Response
import de.enricoe.utils.UploadDeletion
import de.enricoe.utils.getCurrentDate
import io.ktor.http.*
import io.ktor.http.content.*
import kotlinx.coroutines.*
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.litote.kmongo.and
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

object FilesRepository {
    private val deleteZipJobs = hashMapOf<String, Job>()
    private val scope = CoroutineScope(Dispatchers.IO)

    enum class DeleteIn {
        ONE_DAY,
        ONE_WEEK,
        TWO_WEEKS,
        ONE_MONTH,
        THREE_MONTH,
        NEVER;

        fun toTimestamp(uploadTime: LocalDateTime): LocalDateTime {
            return uploadTime.toInstant(TimeZone.currentSystemDefault()).plus(
                when (this) {
                    ONE_DAY -> 2.days
                    ONE_WEEK -> 7.days
                    TWO_WEEKS -> 14.days
                    ONE_MONTH -> 30.days
                    THREE_MONTH -> 90.days
                    NEVER -> (5 * 365).days
                }
            ).toLocalDateTime(TimeZone.currentSystemDefault())
        }
    }

    suspend fun uploadFiles(userId: String, multiPartData: MultiPartData): Response<Any> {
        val uploadTime = getCurrentDate()
        var title = ""
        var password = ""
        var deleteIn = DeleteIn.ONE_MONTH
        val files = arrayListOf<FileUpload>()

        runCatching {
            multiPartData.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        val name = part.name
                        val value = part.value

                        when (name) {
                            "title" -> title = value.trim()
                            "password" -> password = Crypto.hashPassword(value)
                            "deleteIn" -> deleteIn = DeleteIn.valueOf(value)
                        }
                    }

                    is PartData.FileItem -> {
                        // Handle uploaded file
                        val name = part.originalFileName ?: "unknown"
                        val stream = part.streamProvider()
                        val byteArray = stream.readBytes()
                        stream.close()

                        val fileId = FileHasher.hashWithDate(ByteArrayInputStream(byteArray.clone()), getCurrentDate())
                        val directory = File("/uploads/$userId/").also { if (!it.exists()) it.mkdirs() }
                        val targetFile = File(directory, fileId)
                        if (!targetFile.exists()) {
                            Files.copy(
                                ByteArrayInputStream(byteArray.clone()),
                                targetFile.toPath(),
                                StandardCopyOption.REPLACE_EXISTING
                            )
                        }
                        val size = targetFile.length()
                        files.add(FileUpload(fileId, userId, name, size))
                    }

                    else -> {}
                }
                part.dispose()
            }

            if (title.isBlank()) {
                title = files.singleOrNull()?.name ?: "Upload"
            }

            val upload = Upload(userId, title, password.takeIf { it.trim().isNotBlank() }, uploadTime, files.toTypedArray(), deleteIn.toTimestamp(uploadTime))
            MongoManager.uploads.insertOne(upload)
            return Response.Success("$userId/${upload.id}")
        }.onFailure {
            return Response.Error(exception = "Upload failed")
        }
        return Response.Error(exception = "?")
    }

    suspend fun getUpload(userId: String?, author: String, id: String, password: String?): Response<Any> {
        val permissionResult = checkPermission(userId, author, id, password)
        if (permissionResult is Response.Error) return permissionResult
        if (permissionResult is Response.Success) {
            return Response.Success((permissionResult.data as Upload).asResponse())
        }
        return Response.Error()
    }

    suspend fun requestFileDownload(userId: String?, author: String, id: String, fileUpload: FileUpload, password: String?): Response<Any> {
        val permissionResult = checkPermission(userId, author, id, password)
        if (permissionResult is Response.Error) return permissionResult
        if (permissionResult is Response.Success) {
            val upload = permissionResult.data as Upload

            if (upload.files.none { it.id == fileUpload.id } ) {
                return Response.Error(HttpStatusCode.NotFound, "Requested file not found (id)")
            }
            scope.launch {
                incrementDownloads(upload, fileUpload)
            }
            return Response.Success(fileUpload.asFile())
        }
        return Response.Error()
    }

    suspend fun requestDownloadAll(userId: String?, author: String, id: String, password: String?): Response<Any> {
        val permissionResult = checkPermission(userId, author, id, password)
        if (permissionResult is Response.Error) return permissionResult
        if (permissionResult is Response.Success) {
            val upload = permissionResult.data as Upload

            val path = "/uploads/${author}/temp/${id}.zip"
            deleteZipJobs[path]?.cancel()

            val outputFile = File(path)
            deleteZipJobs[path] = scope.launch {
                delay(15.minutes)
                if (outputFile.exists()) {
                    outputFile.parentFile.deleteRecursively()
                }
            }

            fun buildZipFile() {
                if (!outputFile.exists()) {
                    outputFile.parentFile.mkdirs()
                    outputFile.createNewFile()
                    outputFile.deleteOnExit()
                }

                val outputStream = FileOutputStream(outputFile)
                val zipOutput = ZipOutputStream(outputStream)

                for (fileUpload in upload.files) {
                    scope.launch {
                        incrementDownloads(upload, fileUpload)
                    }
                    val file = File("/uploads/$author/${fileUpload.id}")
                    val entry = ZipEntry(fileUpload.name)
                    zipOutput.putNextEntry(entry)
                    zipOutput.write(file.readBytes())
                    zipOutput.closeEntry()
                }
                zipOutput.close()
                outputStream.close()
            }

            buildZipFile()
            return Response.Success(outputFile)
        }
        return Response.Error()
    }

    suspend fun deleteUpload(userId: String?, author: String, id: String): Response<Any> {
        val upload = MongoManager.uploads.findOne(and(Upload::author eq author, Upload::id eq id))
            ?: return Response.Error(HttpStatusCode.NotFound, "Requested Upload not found")
        val user = MongoManager.users.findOne(User::id eq userId)
            ?: return Response.Error(HttpStatusCode.Unauthorized)

        if (!(author == userId || user.role == Role.ADMIN)) {
            return Response.Error(HttpStatusCode.Unauthorized)
        }
        UploadDeletion.delete(upload)
        return Response.Success()
    }

    suspend fun requestAuthorInformation(userId: String?, author: String, id: String, password: String?): Response<Any> {
        val permissionResult = checkPermission(userId, author, id, password)
        if (permissionResult is Response.Error) return permissionResult
        if (author == "Unknown") {
            return Response.Success(AuthorInformationResponse("Unknown", null))
        }
        val authorUser = MongoManager.users.findOne { User::id eq author }
                ?: return Response.Success(AuthorInformationResponse("Unknown", null))
        return Response.Success(AuthorInformationResponse(authorUser.name, authorUser.lastSeen))
    }

    suspend fun getUploads(author: String): Response<Any> {
        return Response.Success(MongoManager.uploads.find(Upload::author eq author).map {
            runBlocking {
                it.asUploadListEntry()
            }
        }.toList())
    }

    private suspend fun incrementDownloads(upload: Upload, fileUpload: FileUpload) {
        val file = upload.files.find { it.id == fileUpload.id } ?: return
        file.downloads += 1
        MongoManager.uploads.replaceOne(Upload::id eq upload.id, upload)
    }

    private fun checkPermission(userId: String?, author: String, id: String, password: String?): Response<Any> {
        val upload = MongoManager.uploads.findOne(and(Upload::author eq author, Upload::id eq id))
                ?: return Response.Error(HttpStatusCode.NotFound, "Requested Upload not found")

        val uploadPassword = upload.password
        if (userId != upload.author && uploadPassword != null && uploadPassword.trim().isNotBlank()) {
            if (password == null || !Crypto.verifyPassword(password, uploadPassword).verified) {
                return Response.Error(HttpStatusCode.Unauthorized,  "Invalid password")
            }
        }
        return Response.Success(upload)
    }
}