package de.enricoe.routes

import de.enricoe.api.requests.FileDownloadRequest
import de.enricoe.api.requests.GetUploadRequest
import de.enricoe.repository.FilesRepository
import de.enricoe.utils.Response
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File

fun Routing.filesRoutes() {
    route("/file") {
        authenticate(optional = true) {
            post("upload") {
                val userId = call.principal<UserIdPrincipal>()?.name ?: "unknown"
                val result = FilesRepository.uploadFiles(userId, call.receiveMultipart())
                call.respond(result.statusCode, result)
            }

            post("getupload") {
                val userId = call.principal<UserIdPrincipal>()?.name
                val request = call.receive<GetUploadRequest>()
                val result = FilesRepository.getUpload(userId, request.author, request.id, request.password)
                call.respond(result.statusCode, result)
            }

            post("requestdownload") {
                val userId = call.principal<UserIdPrincipal>()?.name
                val request = call.receive<FileDownloadRequest>()
                val result = FilesRepository.requestFileDownload(userId, request.author, request.id, request.fileUpload, request.password)

                if (result is Response.Error) {
                    call.respond(result.statusCode, result)
                } else if (result is Response.Success) {
                    call.respondFile(result.data as File)
                }
            }

            post("requestdownloadall") {
                val userId = call.principal<UserIdPrincipal>()?.name
                val request = call.receive<GetUploadRequest>()
                val result = FilesRepository.requestDownloadAll(userId, request.author, request.id, request.password)

                if (result is Response.Error) {
                    call.respond(result.statusCode, result)
                } else if (result is Response.Success) {
                    call.respondFile(result.data as File)
                }
            }

            post("authorinformation") {
                val userId = call.principal<UserIdPrincipal>()?.name
                val request = call.receive<GetUploadRequest>()
                val result = FilesRepository.requestAuthorInformation(userId, request.author, request.id, request.password)
                call.respond(result.statusCode, result)
            }
        }
    }
}