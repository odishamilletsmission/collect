package org.odk.collect.android.wassan.app

import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

// Define the response data models
data class LoginResponse(
    val status: Boolean,
    val message: String,
    val user: User?
)

data class User(
    val id: Int,
    val username: String,
    val email: String,
    val fullname: String,
    val phone: String,
    val position: String,
    val district_id: Int,
    val block_id: Int,
    val gp_id: Int,
    val user_group_id: Int,
    val image: String,
    val user_project: List<UserProject>,
    val default_project: UserProject
)

data class UserProject(
    val id: Int,
    val user_id: Int,
    val organization_id: Int,
    val project_id: String,
    val assign_forms: List<String>,
    val server_id: Int,
    val server_url: String,
    val central_project_id: String,
    val central_user_token: String,
    val project_name: String,
    val color: String,
    val icon: String
)

// Define the login API endpoint
interface ApiService {
    @FormUrlEncoded
    @POST("login")
    fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): Call<LoginResponse>

    // New Clap login
    @FormUrlEncoded
    @POST("claplogin")
    fun clapLogin(
        @Field("email") email: String,
        @Field("password") password: String
    ): Call<LoginResponse>
}
