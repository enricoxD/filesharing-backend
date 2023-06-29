package de.enricoe

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import de.enricoe.database.MongoManager
import de.enricoe.routes.authRoutes
import de.enricoe.routes.filesRoutes
import de.enricoe.routes.userRoutes
import de.enricoe.security.configureSecurity
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

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
    configureSecurity()

    authRoutes()
    userRoutes()
    filesRoutes()

    routing {
        authenticate {
            get("/testurl") {
                call.respond("Working fine")
            }
        }
    }
}
