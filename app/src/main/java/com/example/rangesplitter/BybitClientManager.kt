package com.example.rangesplitter

import bybit.sdk.rest.ByBitRestClient
import bybit.sdk.rest.okHttpClientProvider

object BybitClientManager {
    fun createClient(): ByBitRestClient {
        return ByBitRestClient(
            apiKey = "UV6R9A3gNuk9vl0vVQ",
            secret = "vRdpemzToMITR53ftZSM3ar7kSdx6NeodJTn",
            testnet = true,
            httpClientProvider = okHttpClientProvider
        )
    }
}
