package de.enricoe.routes

import de.enricoe.models.User
import de.enricoe.models.UserRegistrationCredentials
import de.enricoe.repository.UserRepository
import de.enricoe.security.Jwt
import de.enricoe.security.UserSession
import de.enricoe.utils.Response
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*

fun Application.authRoutes() {
    routing {
        route("/auth") {
            post("register") {
                val credentials = call.receive<UserRegistrationCredentials>()
                val result = UserRepository.register(credentials)
                if (result is Response.Success && result.data is User) {
                    call.sessions.set(UserSession(result.data))
                }
                call.respond(result.statusCode, result)
            }

            post("login") {
                val credentials = call.receive<UserPasswordCredential>()
                val result = UserRepository.login(credentials)
                if (result is Response.Success && result.data is User) {
                    call.sessions.set(UserSession(result.data))
                }
                call.respond(result.statusCode, result)
            }

            get("logout") {
                call.sessions.clear("session")
                call.respond(HttpStatusCode.OK)
            }

            authenticate {
                get("validateToken") {
                    call.respond(HttpStatusCode.OK)
                }
            }
        }

        get("user") {
            val session = call.sessions.get<UserSession>() ?: run {
                call.respondText("Not logged in")
                return@get
            }
            call.respondText(session.user.username)
        }
    }
}