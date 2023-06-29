package de.enricoe.utils

import io.ktor.http.*


sealed class Response<T>(
    val statusCode: HttpStatusCode = HttpStatusCode.OK
) {
    data class Success<T>(
        val data: T? = null,
        val message: String? = null
    ): Response<T>()

    data class Error<T>(
        val exception: T? = null,
        val message: String? = null
    ): Response<T>()
}
