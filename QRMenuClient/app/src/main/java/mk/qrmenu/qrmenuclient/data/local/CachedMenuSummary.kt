package mk.qrmenu.qrmenuclient.data.local

data class CachedMenuSummary(
    val userId: String,
    val lastScannedAt: Long,
    val itemCount: Int,
)
