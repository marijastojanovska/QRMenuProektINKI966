package mk.qrmenu.qrmenuclient.model

enum class OrderStatus {
    PENDING,
    ACCEPTED,
    REJECTED;

    companion object {
        fun fromStorage(value: String?): OrderStatus? =
            values().firstOrNull { it.name == value }
    }
}
