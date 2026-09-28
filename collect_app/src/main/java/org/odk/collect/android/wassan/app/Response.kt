package org.odk.collect.android.wassan.app

import org.odk.collect.android.wassan.model.User
import org.odk.collect.android.wassan.model.UserProject

data class LoginResponse(
    val status: Boolean,
    val message: String,
    val user: User?
)

data class UserProjectsResponse(
    val status: Boolean,
    val message: String,
    val userProjects: List<UserProject>?
)