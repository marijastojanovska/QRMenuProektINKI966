package mk.qrmenu.qrmenumanager.model

enum class PaymentMethod {
    CASH,
    CARD;

    companion object {
        fun fromStorage(value: String?): PaymentMethod? =
            values().firstOrNull { it.name == value }
    }
}
