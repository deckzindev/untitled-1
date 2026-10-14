package com.example.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object GitHubApiClient {
  private const val BASE_URL = "https://api.github.com/"

  fun createService(token: String): GitHubApiService {
    val cleanToken = token.trim()

    val authInterceptor = Interceptor { chain ->
      val original = chain.request()
      val builder = original.newBuilder()
        .header("Accept", "application/vnd.github+json")
        .header("X-GitHub-Api-Version", "2022-11-28")
        .header("User-Agent", "GitBuilder-CI-Android")

      if (cleanToken.isNotBlank()) {
        builder.header("Authorization", "Bearer $cleanToken")
      }

      chain.proceed(builder.build())
    }

    val loggingInterceptor = HttpLoggingInterceptor().apply {
      level = HttpLoggingInterceptor.Level.BASIC
    }

    val okHttpClient = OkHttpClient.Builder()
      .addInterceptor(authInterceptor)
      .addInterceptor(loggingInterceptor)
      .followRedirects(true)
      .followSslRedirects(true)
      .connectTimeout(60, TimeUnit.SECONDS)
      .readTimeout(180, TimeUnit.SECONDS)
      .writeTimeout(60, TimeUnit.SECONDS)
      .build()

    val moshi = Moshi.Builder()
      .addLast(KotlinJsonAdapterFactory())
      .build()

    val retrofit = Retrofit.Builder()
      .baseUrl(BASE_URL)
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()

    return retrofit.create(GitHubApiService::class.java)
  }
}
