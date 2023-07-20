package de.enricoe.api.requests

class UserUpdateData(
    var name: String? = null,
    var email: String? = null,
    var newPassword: String? = null,
    var currentPassword: String? = null,
)