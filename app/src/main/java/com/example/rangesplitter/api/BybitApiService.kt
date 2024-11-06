import BalanceResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query
import retrofit2.http.Header

// Define API Interface
// Define API Interface
interface BybitApiService {

    @GET("/v5/account/wallet-balance")
    suspend fun getWalletBalance(
        @Header("X-BAPI-API-KEY") apiKey: String,
        @Header("X-BAPI-TIMESTAMP") timestamp: String,
        @Header("X-BAPI-SIGN") signature: String,
        @Query("accountType") accountType: String,
        @Query("demo") demo: Boolean = true,
        @Query("coin") coin: String
    ): BalanceResponse

    // Add this function to get the server time
    @GET("/v5/common/time")
    suspend fun getServerTime(): ServerTimeResponse

    companion object {
        private const val BASE_URL = "https://api-demo.bybit.com"

        // Singleton instance of Retrofit API service
        val api: BybitApiService by lazy {
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(BybitApiService::class.java)
        }
    }
}

