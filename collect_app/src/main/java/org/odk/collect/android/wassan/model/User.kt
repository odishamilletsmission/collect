package org.odk.collect.android.wassan.model

import com.google.gson.annotations.SerializedName

data class User(
    val id: Int,
    val username: String,
    val email: String,
    val fullname: String,
    val phone: String,
    val position: String,
    @SerializedName("district_id") val districtId: Int,
    @SerializedName("block_id") val blockId: Int,
    @SerializedName("gp_id") val gpId: Int,
    @SerializedName("user_group_id") val userGroupId: Int,
    val image: String,
    @SerializedName("user_project") val userProjects: List<UserProject>?,
    @SerializedName("default_project") val defaultProject: UserProject?
)

