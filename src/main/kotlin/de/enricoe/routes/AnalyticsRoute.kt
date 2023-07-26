package de.enricoe.routes

import de.enricoe.api.responses.analytics.PublicInformationResponse
import de.enricoe.database.MongoManager
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Routing.analyticsRoutes() {
    route("/analytics") {
        get("public-information") {
            val uploads = MongoManager.uploads.find().sumOf { upload -> upload.files.size }
            val downloads = MongoManager.uploads.find().sumOf { upload ->
                upload.files.sumOf { file -> file.downloads }
            }
            val users = MongoManager.users.countDocuments()
            call.respond(PublicInformationResponse(uploads, downloads, users))
        }
    }
}