package mk.qrmenu.qrmenuclient.data

import kotlinx.coroutines.flow.Flow
import mk.qrmenu.qrmenuclient.data.local.CachedMenuDao
import mk.qrmenu.qrmenuclient.data.local.CachedMenuEntity
import mk.qrmenu.qrmenuclient.data.local.CachedMenuItemEntity
import mk.qrmenu.qrmenuclient.data.local.CachedMenuSummary
import mk.qrmenu.qrmenuclient.model.Product

class CachedMenuRepository(private val dao: CachedMenuDao) {

    fun observeSummaries(): Flow<List<CachedMenuSummary>> = dao.observeSummaries()

    suspend fun getMenu(userId: String): List<Product> =
        dao.getItems(userId).map { it.toProduct() }

    suspend fun saveMenu(userId: String, items: List<Product>, scannedAt: Long) {
        val menu = CachedMenuEntity(userId = userId, lastScannedAt = scannedAt)
        val entities = items.map { it.toEntity(userId) }
        dao.saveMenu(menu, entities)
    }

    private fun CachedMenuItemEntity.toProduct() = Product(
        id = productId,
        title = title,
        description = description,
        price = price,
        imageUrl = imageUrl,
        category = category,
    )

    private fun Product.toEntity(ownerUserId: String) = CachedMenuItemEntity(
        ownerUserId = ownerUserId,
        productId = id,
        title = title,
        description = description,
        price = price,
        imageUrl = imageUrl,
        category = category,
    )
}
