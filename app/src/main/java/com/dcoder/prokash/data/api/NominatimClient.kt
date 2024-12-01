package com.dcoder.prokash.data.api

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NominatimClient {
    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "prokash/1.0 (mirzatawhid404@gmail.com)")
                .build()
            chain.proceed(request)
        }
        .build()


    private val retrofit = Retrofit.Builder()
        .baseUrl("https://nominatim.openstreetmap.org/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service: NominatimService = retrofit.create(NominatimService::class.java)
}