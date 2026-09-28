package org.odk.collect.android.wassan.model

import com.google.gson.annotations.SerializedName

data class UserProject(
    val id: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("organization_id") val organizationId: Int,
    @SerializedName("project_id") val projectId: String,
    @SerializedName("assign_forms") val assignForms: List<String>,
    @SerializedName("server_id") val serverId: Int,
    @SerializedName("server_url") val serverUrl: String,
    @SerializedName("central_project_id") val centralProjectId: String,
    @SerializedName("central_user_token") val centralUserToken: String,
    @SerializedName("project_name") val projectName: String,
    val color: String,
    val icon: String
)