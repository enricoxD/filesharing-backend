package de.enricoe.security

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import java.util.*

object Jwt {
    private lateinit var secret: String
    private lateinit var issuer: String
    lateinit var audience: String
    lateinit var realm: String
    private val validityInMs = 36_000_00 * 24 // 24 hours
    private lateinit var algorithm: Algorithm
    const val CLAIM = "id"

    lateinit var verifier: JWTVerifier

    fun init(application: Application) {
        secret = application.environment.config.property("jwt.secret").getString()
        issuer = application.environment.config.property("jwt.issuer").getString()
        audience = application.environment.config.property("jwt.audience").getString()
        realm = application.environment.config.property("jwt.realm").getString()
        algorithm = Algorithm.HMAC256(secret)
        verifier = JWT.require(algorithm)
            .withIssuer(issuer)
            .withAudience(audience)
            .build()
    }

    fun makeToken(id: String): String = JWT.create()
        .withIssuer(issuer)
        .withAudience(audience)
        .withClaim(CLAIM, id)
        .withExpiresAt(Date(System.currentTimeMillis() + validityInMs))
        .sign(algorithm)

}