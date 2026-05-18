package mk.qrmenu.qrmenumanager.model

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class Order(
    @DocumentId var id: String = "",
    var clientId: String = "",
    var managerId: String = "",
    var status: String = OrderStatus.PENDING.name,
    var items: List<OrderItem> = emptyList(),
    @ServerTimestamp var createdAt: Date? = null,
) : Parcelable
