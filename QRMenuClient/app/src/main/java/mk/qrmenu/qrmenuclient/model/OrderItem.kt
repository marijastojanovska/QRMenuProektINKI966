package mk.qrmenu.qrmenuclient.model

data class OrderItem(
    var productId: String = "",
    var title: String = "",
    var price: Double = 0.0,
    var quantity: Int = 0,
)
