package org.odk.collect.android.wassan.app

import android.content.Context
import okhttp3.OkHttpClient
import org.odk.collect.strings.R
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import timber.log.Timber

object ApiClient {
    @Volatile
    private var retrofit: Retrofit? = null

    fun create(context: Context): ApiService {
        return getRetrofit(context).create(ApiService::class.java)
    }

    private fun getRetrofit(context: Context): Retrofit {
        return retrofit ?: synchronized(this) {
            retrofit ?: buildRetrofit(context).also { retrofit = it }
        }
    }

    private fun buildRetrofit(context: Context): Retrofit {
        val baseUrl = context.applicationContext.getString(R.string.api_server_url)

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                Timber.d("Sending request to %s", request.url())
                val response = chain.proceed(request)
                Timber.d("Received response from %s with code %d", response.request().url(), response.code())
                response
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
