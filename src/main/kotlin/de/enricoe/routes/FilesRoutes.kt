package de.enricoe.routes

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

            get("{author}/{hash}") {
                runCatching {
                    val user = call.sessions.get<UserSession>()?.user
                    val author = call.parameters["author"]!!
                    val hash = call.parameters["hash"]!!
                    val result = FilesRepository.getUpload(user, null, author, hash)
                    call.respond(result.statusCode, result)
                    return@get
                }.onFailure {
                    call.respond(HttpStatusCode.BadRequest)
                }
            }
        }
    }
}