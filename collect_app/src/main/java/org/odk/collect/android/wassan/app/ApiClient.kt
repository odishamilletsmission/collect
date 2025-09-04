package org.odk.collect.android.wassan.app

import android.content.Context
import okhttp3.OkHttpClient
import org.odk.collect.strings.R
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    fun create(context: Context): ApiService {
        val baseUrl = context.getString(R.string.api_server_url)

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(ApiService::class.java)
    }
}