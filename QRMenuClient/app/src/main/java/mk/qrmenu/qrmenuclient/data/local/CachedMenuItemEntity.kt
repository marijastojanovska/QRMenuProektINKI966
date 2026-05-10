package mk.qrmenu.qrmenuclient.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "cached_menu_items",
    primaryKeys = ["ownerUserId", "productId"],
    foreignKeys = [
        ForeignKey(
            entity = CachedMenuEntity::class,
            parentColumns = ["userId"],
            childColumns = ["ownerUserId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("ownerUserId")],
)
data class CachedMenuItemEntity(
    val ownerUserId: String,
    val productId: String,
    val title: String,
    val description: String,
    val price: Double,
    val imageUrl: String,
    val category: String,
)
