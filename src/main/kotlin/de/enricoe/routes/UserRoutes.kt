package de.enricoe.routes

import de.enricoe.models.User
import de.enricoe.repository.UserRepository
import de.enricoe.security.UserSession
import de.enricoe.utils.Response
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class UpdatePasswordRequest(
    val newPassword: String,
    val currentPassword: String,
)

@Serializable
data class UpdateCredentialRequest(
    val newCredential: String
)

fun Routing.userRoutes() {
    route("/user") {
        post("updateUsername") {
            val session = call.sessions.get<UserSession>() ?: run {
                call.respond(HttpStatusCode.Unauthorized)
                return@post
            }
            val user = session.user
            val newUsername = call.receive<UpdateCredentialRequest>().newCredential
            val result = UserRepository.updateUsername(user, newUsername)
            if (result is Response.Success && result.data is User) {
                call.sessions.set(UserSession(result.data))
            }
            call.respond(result.statusCode, result)
        }

        post("updateEmail") {
            val session = call.sessions.get<UserSession>() ?: run {
                call.respond(HttpStatusCode.Unauthorized)
                return@post
            }
            val user = session.user
            val newEmail = call.receive<UpdateCredentialRequest>().newCredential
            val result = UserRepository.updateEmail(user, newEmail)
            if (result is Response.Success && result.data is User) {
                call.sessions.set(UserSession(result.data))
            }
            call.respond(result.statusCode, result)
        }

        post("updatePassword") {
            val session = call.sessions.get<UserSession>() ?: run {
                call.respond(HttpStatusCode.Unauthorized)
                return@post
            }
            val user = session.user
            val credentials = call.receive<UpdatePasswordRequest>()
            val result =
                UserRepository.updatePassword(user, credentials.newPassword, credentials.currentPassword)
            if (result is Response.Success && result.data is User) {
                call.sessions.set(UserSession(result.data))
            }
            call.respond(result.statusCode, result)
        }

        post("updateLastSeen") {
            val session = call.sessions.get<UserSession>() ?: run {
                call.respond(HttpStatusCode.Unauthorized)
                return@post
            }
            val user = session.user
            user.lastSeen = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        }

        get("deleteUser") {

        }

        get("current-user") {
            val userId = call.principal<UserIdPrincipal>()?.name
            if (userId == null) {
                call.respond(Response.Error(null, "You're not logged in"))
                return@get
            }
            val user = User.getById(userId)
            call.respond(Response.Success(user))
        }
    }
}