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

fun Routing.authRoutes() {
    route("/auth") {
        post("register") {
            val credentials = call.receive<UserRegistrationCredentials>()
            val result = UserRepository.register(call, credentials)
            if (result is Response.Success && result.data is User) {
                //call.sessions.set(UserSession(result.data))
                call.respond(result.statusCode, result.data)
                return@post
            }
            if (result is Response.Error) {
                call.respond(result.statusCode, result)
                return@post
            }
            call.respond(result.statusCode)
        }

        post("login") {
            val credentials = call.receive<UserPasswordCredential>()
            val result = UserRepository.login(call, credentials)
            if (result is Response.Success && result.data is User) {
                //call.sessions.set(UserSession(result.data))
                call.respond(result.statusCode, result.data)
                return@post
            }
            if (result is Response.Error) {
                call.respond(result.statusCode, result)
                return@post
            }
            call.respond(result.statusCode)
        }

        get("logout") {
            call.sessions.clear("session")
            call.response.cookies.append(Cookie("headerPayload", "", maxAge = 0, secure = true, httpOnly = false, path = "/"))
            call.response.cookies.append(Cookie("signature", "", maxAge = 0, secure = true, httpOnly = false, path = "/"))
            call.respond(HttpStatusCode.OK)
        }
    }
}