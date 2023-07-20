package de.enricoe.repository

import de.enricoe.api.responses.AuthorInformationResponse
import de.enricoe.database.MongoManager
import de.enricoe.models.FileUpload
import de.enricoe.models.Upload
import de.enricoe.models.User
import de.enricoe.security.Crypto
import de.enricoe.utils.FileHasher
import de.enricoe.utils.Response
import io.ktor.http.*
import io.ktor.http.content.*
import kotlinx.coroutines.*
import kotlinx.datetime.*
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
    val deleteZipJobs = hashMapOf<String, Job>()

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

    suspend fun uploadFiles(userId: String, multiPartData: MultiPartData): Response<Any> {
        val uploadTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
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
                            //"deleteIn" -> deleteIn = DeleteIn.valueOf(value)
                        }
                    }

                    is PartData.FileItem -> {
                        // Handle uploaded file
                        val name = part.originalFileName ?: "unknown"
                        val stream = part.streamProvider()
                        val byteArray = stream.readBytes()
                        stream.close()

                        val hash = FileHasher.hash(ByteArrayInputStream(byteArray.clone()))
                        val directory = File("/uploads/$userId/").also { if (!it.exists()) it.mkdirs() }
                        val targetFile = File(directory, hash)
                        if (!targetFile.exists()) {
                            Files.copy(
                                ByteArrayInputStream(byteArray.clone()),
                                targetFile.toPath(),
                                StandardCopyOption.REPLACE_EXISTING
                            )
                        }
                        val size = targetFile.length()
                        files.add(FileUpload(name, hash, size))
                    }

                    else -> {}
                }
                part.dispose()
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

            if (upload.files.none { it.hash == fileUpload.hash } ) {
                return Response.Error(HttpStatusCode.NotFound, "Requested Upload not found (hash)")
            }

            return Response.Success(File("/uploads/${author}/${fileUpload.hash}"))
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
            deleteZipJobs[path] = CoroutineScope(Dispatchers.IO).launch {
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
                    val file = File("/uploads/$author/${fileUpload.hash}")
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

    fun checkPermission(userId: String?, author: String, id: String, password: String?): Response<Any> {
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