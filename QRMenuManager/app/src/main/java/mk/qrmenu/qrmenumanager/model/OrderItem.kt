package mk.qrmenu.qrmenumanager.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class OrderItem(
    var productId: String = "",
    var title: String = "",
    var price: Double = 0.0,
    var quantity: Int = 0,
) : Parcelable
