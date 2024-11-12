package com.yourapp.network

import ApiService
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
//    private const val BASE_URL = "https://hbssyluucrwsbfzspyfp.supabase.co/rest/v1/"
//    private const val API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imhic3N5bHV1Y3J3c2JmenNweWZwIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Mjk2NTU4OTEsImV4cCI6MjA0NTIzMTg5MX0.o6fkro2tPKFoA9sxAp1nuseiHRGiDHs_HI4-ZoqOTfQ"


    private const val BASE_URL = "http://poultrylink.ambatuwin.xyz/api/"
//    private const val BASE_URL = "http://192.168.3.233:8000/api/"

    // Replace with your method of getting the token (e.g., from SharedPreferences, or session)
    private var userToken: String? = null

    // Function to set the token, you can call this when the user logs in
    fun setUserToken(token: String) {
        userToken = token
    }



    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            // Check if the token is available
            val requestBuilder = chain.request().newBuilder()

//            userToken?.let {
//                // Add the Authorization header with Bearer token
//                requestBuilder.addHeader("Authorization", "Bearer $it")
//            }

            // If you need to add additional headers, you can do so here
            // requestBuilder.addHeader("apikey", "YourAPIKeyHere") // If needed

            val request: Request = requestBuilder.build()
            chain.proceed(request)
        }
        .build()

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
