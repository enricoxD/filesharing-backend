package de.enricoe

import com.auth0.jwt.exceptions.TokenExpiredException
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import de.enricoe.database.MongoManager
import de.enricoe.routes.authRoutes
import de.enricoe.routes.filesRoutes
import de.enricoe.routes.userRoutes
import de.enricoe.security.Jwt
import de.enricoe.security.configureSecurity
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.forwardedheaders.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    MongoManager.init()
    install(CORS) {
        allowCredentials = true
        allowSameOrigin = true
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.AccessControlAllowOrigin)
    }
    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
        }
    }
    install(ForwardedHeaders) // WARNING: for security, do not include this if not behind a reverse proxy
    install(XForwardedHeaders) // WARNING: for security, do not include this if not behind a reverse proxy
    configureSecurity()

    routing {
        intercept(ApplicationCallPipeline.Setup) {
            val headerPayload = call.request.cookies["headerPayload"]
            val signature = call.request.cookies["signature"]
            var id = ""
            if (headerPayload != null && signature != null) {
                try {
                    val token = Jwt.verifier.verify("$headerPayload.$signature")
                    id = token.getClaim(Jwt.CLAIM).asString()
                    call.authentication.principal(UserIdPrincipal(id))
                } catch (_: Exception) {}

            }
        }
        authRoutes()
        userRoutes()
        filesRoutes()
    }
}
