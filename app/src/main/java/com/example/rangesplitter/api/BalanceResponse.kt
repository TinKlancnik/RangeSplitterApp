data class BalanceResponse(
    val retCode: Int,
    val retMsg: String,
    val result: Result,
    val retExtInfo: Any? // or create a specific class if needed
)

data class Result(
    val memberId: String,
    val accountType: String,
    val balance: List<CoinBalance> // Change to balance instead of list
)

data class CoinBalance(
    val coin: String,
    val transferBalance: String,
    val walletBalance: String,
    val bonus: String? // This is optional
)
