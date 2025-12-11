package com.example.rangesplitter

import bybit.sdk.rest.ByBitRestClient
import bybit.sdk.rest.okHttpClientProvider   // 👈 THIS is the important import

object BybitClientManager {

    // One shared client for the whole app
    val client: ByBitRestClient by lazy {
        ByBitRestClient(
            apiKey = "UV6R9A3gNuk9vl0vVQ",
            secret = "vRdpemzToMITR53ftZSM3ar7kSdx6NeodJTn",
            testnet = true,
            httpClientProvider = okHttpClientProvider   // 👈 same as in your SplitFragment before
        )
    }
}