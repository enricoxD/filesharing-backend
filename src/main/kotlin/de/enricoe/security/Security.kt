package de.enricoe.security

import de.enricoe.api.responses.UserResponse
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.sessions.*
import io.ktor.server.sessions.serialization.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.days

@Serializable
data class UserSession(val user: UserResponse)

fun Application.configureSecurity() {
    Jwt.init(this)
    install(Sessions) {
        //val secretEncryptKey = hex(this@configureSecurity.environment.config.property("session.encryptKey").getString())
        //val secretSignKey = hex(this@configureSecurity.environment.config.property("session.signKey").getString())

        cookie<UserSession>("user_session") {
            serializer = KotlinxSessionSerializer(Json)
            cookie.maxAge = 30.days
            cookie.extensions["SameSite"] = "lax"
            cookie.secure = true
            //transform(SessionTransportTransformerEncrypt(secretEncryptKey, secretSignKey))
        }
    }

    install(Authentication) {
        jwt {
            verifier(Jwt.verifier)
            validate {
                val headerPayload = request.cookies["headerPayload"] ?: return@validate null
                val signature = request.cookies["signature"] ?: return@validate null
                val tokenRepresentation = "$headerPayload.$signature"
                val token = Jwt.verifier.verify(tokenRepresentation)

                val claim = token.getClaim(Jwt.CLAIM).asString()
                if (claim != null) {
                    UserIdPrincipal(claim)
                } else {
                    null
                }
            }
        }
    }

}