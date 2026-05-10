package mk.qrmenu.qrmenuclient.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_menus")
data class CachedMenuEntity(
    @PrimaryKey val userId: String,
    val lastScannedAt: Long,
)
