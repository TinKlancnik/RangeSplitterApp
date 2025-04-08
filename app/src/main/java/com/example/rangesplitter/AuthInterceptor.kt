package com.example.rangesplitter.api

import okhttp3.Interceptor
import okhttp3.Response
import java.net.URLEncoder
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class AuthInterceptor(
    private val apiKey: String,
    private val apiSecret: String
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val originalUrl = request.url

        val timestamp = System.currentTimeMillis().toString()

        // Collect only required parameters (exclude apiKey)
        val queryParams = originalUrl.queryParameterNames
            .associateWith { originalUrl.queryParameter(it).orEmpty() }
            .toMutableMap()

        queryParams["timestamp"] = timestamp

        // Create signature
        val signature = createSignature(queryParams)

        // Build a new URL **without modifying existing query parameters**
        val newUrl = originalUrl.newBuilder().build()

        // Build final request **with headers instead of URL parameters**
        val newRequest = request.newBuilder()
            .url(newUrl)
            .addHeader("X-BAPI-API-KEY", apiKey)
            .addHeader("X-BAPI-SIGN", signature)
            .addHeader("X-BAPI-TIMESTAMP", timestamp)
            .addHeader("X-BAPI-RECV-WINDOW", "5000")
            .build()

        return chain.proceed(newRequest)
    }

    private fun createSignature(params: Map<String, String>): String {
        val sortedParams = params.toSortedMap()
        val queryString = sortedParams.map {
            "${it.key}=${URLEncoder.encode(it.value, "UTF-8")}"
        }.joinToString("&")

        val mac = Mac.getInstance("HmacSHA256").apply {
            init(SecretKeySpec(apiSecret.toByteArray(), "HmacSHA256"))
        }

        return mac.doFinal(queryString.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
