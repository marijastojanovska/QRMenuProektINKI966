package mk.qrmenu.qrmenuclient.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Order(
    @DocumentId var id: String = "",
    var clientId: String = "",
    var managerId: String = "",
    var status: String = OrderStatus.PENDING.name,
    var items: List<OrderItem> = emptyList(),
    @ServerTimestamp var createdAt: Date? = null,
)
