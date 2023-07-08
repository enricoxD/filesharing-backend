package de.enricoe.routes

import de.enricoe.api.requests.GetUploadRequest
import de.enricoe.repository.FilesRepository
import de.enricoe.security.UserSession
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*

fun Application.filesRoutes() {
    routing {
        route("/file") {
            post("upload") {
                val author = call.sessions.get<UserSession>()?.user?.id ?: "unknown"
                val result = FilesRepository.uploadFiles(author, call.receiveMultipart())
                call.respond(result.statusCode, result)
            }

            post("getupload") {
                runCatching {
                    val user = call.sessions.get<UserSession>()?.user
                    val request = call.receive<GetUploadRequest>()
                    val result = FilesRepository.getUpload(user, request.author, request.id, request.password)
                    call.respond(result.statusCode, result)
                    return@post
                }.onFailure {
                    call.respond(HttpStatusCode.BadRequest)
                }
            }
        }
    }
}