package bybit.sdk.rest.account

import bybit.sdk.ext.coroutineToRestCallback
import bybit.sdk.rest.ByBitRestApiCallback
import bybit.sdk.rest.ByBitRestClient
import kotlinx.coroutines.runBlocking

class ByBitAccountClient
internal constructor(internal val byBitRestClient: ByBitRestClient) {

    fun getWalletBalanceBlocking(params: WalletBalanceParams):
            WalletBalanceResponse = runBlocking { getWalletBalance(params) }

    fun getWalletBalance(
        params: WalletBalanceParams,
        callback: ByBitRestApiCallback<WalletBalanceResponse>
    ) = coroutineToRestCallback(callback, { getWalletBalance(params) })

    fun getFeeRateBlocking(params: FeeRateParams):
            FeeRateResponse = runBlocking { getFeeRate(params) }

    fun getFeeRate(
        params: FeeRateParams,
        callback: ByBitRestApiCallback<FeeRateResponse>
    ) = coroutineToRestCallback(callback, { getFeeRate(params) })


}
