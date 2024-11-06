package com.example.rangesplitter.api

import BalanceResponse
import android.util.Log
import retrofit2.HttpException
import java.net.URLEncoder
import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.text.Charsets.UTF_8

class BybitRepository {

    // Function to fetch wallet balance
    suspend fun getWalletBalance(apiKey: String, apiSecret: String): Double? {
        return try {
            // Fetch server time
            val timestamp = getTimestamp().toString()

            // Create params for signature
            val params = mapOf(
                "accountType" to "UNIFIED",
                "coin" to "USDT"
            )

            // Generate the API signature
            val signature = generateSignature(apiKey, apiSecret, timestamp.toLong(), params)

            // Log request parameters
            Log.d("API Request", "API Key: $apiKey")
            Log.d("API Request", "Timestamp: $timestamp")
            Log.d("API Request", "Signature: $signature")
            Log.d("API Request", "Account Type: UNIFIED")
            Log.d("API Request", "Coin: USDT")

            // Make API call to get the balance
            val response = BybitApiService.api.getWalletBalance(
                apiKey = apiKey,
                signature = signature,
                timestamp = timestamp,
                accountType = "UNIFIED",
                coin = "USDT"
            )

            Log.d("API Response", "Response: $response")

            // Parse the response to extract the wallet balance
            parseBalanceResponse(response)

        } catch (e: HttpException) {
            // Log the HTTP exception response and status code
            val errorBody = e.response()?.errorBody()?.string()
            Log.e("API Error", "HTTP Error: ${e.code()}, Message: ${e.message()}, Response: $errorBody")
            null
        } catch (e: Exception) {
            // Log the general exception with stack trace
            Log.e("API Error", "Error fetching balance", e)
            null
        }
    }


    private suspend fun getServerTime(): Long? {
        return try {
            val response = BybitApiService.api.getServerTime()
            response.time
        } catch (e: HttpException) {
            Log.e("API Error", "Error fetching server time", e)
            null
        }
    }

    // Function to get the current timestamp
    fun getTimestamp(): Long {
        return System.currentTimeMillis() // This works on all API levels
    }

    fun urlEncode(value: String): String {
        return value.map {
            when (it) {
                ' ' -> "+" // Encode spaces as +
                else -> it.toString()
            }
        }.joinToString("").replace(Regex("[^\\w\\+\\.\\-\\~]")) { match ->
            "%${match.value.toByteArray(UTF_8).joinToString("") { byte -> "%02X".format(byte) }}"
        }
    }


    // Function to generate HMAC SHA-256 signature
    fun generateSignature(apiKey: String, secretKey: String, timestamp: Long, params: Map<String, String>): String {
        // Sort the parameters alphabetically
        val sortedParams = params.toSortedMap()

        // Prepare the parameter string in the required order
        val stringBuilder = StringBuilder()
        stringBuilder.append(timestamp)
        stringBuilder.append(apiKey)

        // Encode and append each parameter
        sortedParams.forEach { (key, value) ->
            stringBuilder.append(urlEncode(key))
            stringBuilder.append("=")
            stringBuilder.append(urlEncode(value))
        }

        // Convert the final string to a byte array for HMAC generation
        val paramStr = stringBuilder.toString()

        // Initialize the HMAC SHA-256 Mac instance
        val hmacSha256 = Mac.getInstance("HmacSHA256")
        val secretKeySpec = SecretKeySpec(secretKey.toByteArray(UTF_8), "HmacSHA256")
        hmacSha256.init(secretKeySpec)

        // Perform HMAC on the parameter string
        val hash = hmacSha256.doFinal(paramStr.toByteArray(UTF_8))

        // Return the signature in hexadecimal format
        return hash.joinToString("") { "%02x".format(it) }
    }

    // Function to parse balance response
    private fun parseBalanceResponse(responseBody: BalanceResponse?): Double? {
        if (responseBody == null || responseBody.result == null || responseBody.result.balance == null) {
            Log.e("Parse Error", "Result or balance is null. Response: $responseBody")
            return null
        }

        // Iterate over the balances
        responseBody.result.balance?.forEach { coinBalance ->
            if (coinBalance.coin == "USDT") {
                return coinBalance.walletBalance.toDoubleOrNull() // Convert to Double safely
            }
        }
        return null
    }
}

