package mk.qrmenu.qrmenuclient.model

enum class PaymentMethod {
    CASH,
    CARD;

    companion object {
        fun fromStorage(value: String?): PaymentMethod? =
            values().firstOrNull { it.name == value }
    }
}
