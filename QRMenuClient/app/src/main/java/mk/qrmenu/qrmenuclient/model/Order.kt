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
    var customerAddress: String = "",
    var customerCity: String = "",
    var customerPhone: String = "",
    var paymentMethod: String = PaymentMethod.CASH.name,
    @ServerTimestamp var createdAt: Date? = null,
)
