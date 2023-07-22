package de.enricoe.routes

import de.enricoe.api.requests.GetUserRequest
import de.enricoe.models.User
import de.enricoe.repository.FilesRepository
import de.enricoe.repository.UserRepository
import de.enricoe.utils.Response
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Routing.userRoutes() {
    route("/user") {
        authenticate {
            post("update") {
                val userId = call.principal<UserIdPrincipal>()?.name ?: run {
                    val error = Response.Error(HttpStatusCode.Unauthorized, null)
                    call.respond(error.statusCode, error)
                    return@post
                }
                val result = UserRepository.updateUser(userId, call.receiveMultipart())
                call.respond(result.statusCode, result)
            }
        }

        post("get-uploads") {
            val userId = call.principal<UserIdPrincipal>()?.name ?: run {
                val error = Response.Error(HttpStatusCode.Unauthorized, null)
                call.respond(error.statusCode, error)
                return@post
            }
            val requestedUser = call.receive<GetUserRequest>().id
            if (requestedUser != userId) {
                val error = Response.Error(HttpStatusCode.Unauthorized, null)
                call.respond(error.statusCode, error)
                return@post
            }
            val response = FilesRepository.getUploads(requestedUser) as Response.Success
            call.respond(response.statusCode, response)
        }

        get("current-user") {
            val userId = call.principal<UserIdPrincipal>()?.name
            if (userId == null) {
                call.respond(Response.Error(exception = "You're not logged in"))
                return@get
            }
            val user = User.getById(userId)
            call.respond(Response.Success(user?.asResponse()))
        }

        get("avatar/{user}") {
            val user = call.parameters["user"]
            call.respondFile(UserRepository.getAvatar(user))
        }
    }
}