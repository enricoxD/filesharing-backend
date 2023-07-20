package de.enricoe.utils

import io.ktor.http.*


sealed class Response<T>(
    open val statusCode: HttpStatusCode = HttpStatusCode.OK,
) {
    data class Success<T>(
        val data: T? = null,
        val message: String? = null
    ) : Response<T>()

    data class Error(
        override val statusCode: HttpStatusCode = HttpStatusCode.OK,
        val exception: String? = null
    ) : Response<Any>()
}
