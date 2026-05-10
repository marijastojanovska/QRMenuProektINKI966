package mk.qrmenu.qrmenuclient.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedMenuDao {

    @Query(
        """
        SELECT m.userId AS userId,
               m.lastScannedAt AS lastScannedAt,
               (SELECT COUNT(*) FROM cached_menu_items i WHERE i.ownerUserId = m.userId) AS itemCount
        FROM cached_menus m
        ORDER BY m.lastScannedAt DESC
        """
    )
    fun observeSummaries(): Flow<List<CachedMenuSummary>>

    @Query("SELECT * FROM cached_menu_items WHERE ownerUserId = :userId")
    suspend fun getItems(userId: String): List<CachedMenuItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMenu(menu: CachedMenuEntity)

    @Query("DELETE FROM cached_menu_items WHERE ownerUserId = :userId")
    suspend fun deleteItems(userId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<CachedMenuItemEntity>)

    @Transaction
    suspend fun saveMenu(menu: CachedMenuEntity, items: List<CachedMenuItemEntity>) {
        upsertMenu(menu)
        deleteItems(menu.userId)
        if (items.isNotEmpty()) {
            insertItems(items)
        }
    }
}
