package cl.fuerzarank.core

enum class TxType { EARN, SPEND }

data class CoinTx(
    val id: String, val profileId: String, val type: TxType, val amount: Int,
    val reason: String, val idempotencyKey: String
)

sealed interface Result2 {
    data class Ok(val tx: CoinTx) : Result2
    data object Duplicate : Result2
    data object InsufficientFunds : Result2
    data object AlreadyOwned : Result2
}

data class ShopItem(val id: String, val name: String, val category: String, val price: Int, val rarity: String = "común")

/** Libro contable en memoria; la versión Room implementará el mismo contrato con UNIQUE(idempotencyKey). */
class Wallet {
    private val txs = mutableListOf<CoinTx>()
    private val owned = mutableSetOf<Pair<String, String>>() // (profileId, itemId)
    private var seq = 0

    fun history(profileId: String) = txs.filter { it.profileId == profileId }
    fun balance(profileId: String) = history(profileId).sumOf { if (it.type == TxType.EARN) it.amount else -it.amount }
    fun owns(profileId: String, itemId: String) = (profileId to itemId) in owned

    /** Una recompensa por clave: reabrir la app o doble toque no duplica. */
    fun earn(profileId: String, amount: Int, reason: String, key: String): Result2 {
        require(amount > 0)
        if (txs.any { it.idempotencyKey == key }) return Result2.Duplicate
        return Result2.Ok(CoinTx("tx${++seq}", profileId, TxType.EARN, amount, reason, key).also { txs += it })
    }

    fun buy(profileId: String, item: ShopItem): Result2 {
        if (owns(profileId, item.id)) return Result2.AlreadyOwned
        if (balance(profileId) < item.price) return Result2.InsufficientFunds
        val tx = CoinTx("tx${++seq}", profileId, TxType.SPEND, item.price, "Compra ${item.name}", "buy:$profileId:${item.id}")
        txs += tx; owned += profileId to item.id
        return Result2.Ok(tx)
    }
}
