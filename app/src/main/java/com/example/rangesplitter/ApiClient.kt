package com.example.rangesplitter.api

import okhttp3.*
import java.io.IOException

class ApiClient(private val apiKey: String, private val apiSecret: String) {

    private val client = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(apiKey, apiSecret)) // Add AuthInterceptor
        .build()

    fun makeRequest(endpoint: String, callback: (String?, Exception?) -> Unit) {
        val url = "https://api.bybit.com$endpoint"
        val request = Request.Builder()
            .url(url)
            .get() // Use .post() for POST requests
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(null, e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        callback(null, IOException("Unexpected response: $it"))
                    } else {
                        callback(it.body?.string(), null)
                    }
                }
            }
        })
    }
}
