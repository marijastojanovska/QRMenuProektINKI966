package mk.qrmenu.qrmenuclient.cart

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import mk.qrmenu.qrmenuclient.model.OrderItem
import mk.qrmenu.qrmenuclient.model.Product

data class CartState(
    val managerId: String? = null,
    val entries: List<OrderItem> = emptyList(),
) {
    val totalQuantity: Int get() = entries.sumOf { it.quantity }
    val totalPrice: Double get() = entries.sumOf { it.price * it.quantity }
    val isEmpty: Boolean get() = entries.isEmpty()
}

object CartRepository {

    private val _state = MutableStateFlow(CartState())
    val state: StateFlow<CartState> = _state.asStateFlow()

    fun bindToManager(managerId: String) {
        val current = _state.value
        if (current.managerId != managerId) {
            _state.value = CartState(managerId = managerId)
        }
    }

    fun add(product: Product) {
        val current = _state.value
        val managerId = current.managerId ?: return
        val list = current.entries.toMutableList()
        val idx = list.indexOfFirst { it.productId == product.id }
        if (idx >= 0) {
            val existing = list[idx]
            list[idx] = existing.copy(quantity = existing.quantity + 1)
        } else {
            list += OrderItem(
                productId = product.id,
                title = product.title,
                price = product.price,
                quantity = 1,
            )
        }
        _state.value = current.copy(managerId = managerId, entries = list)
    }

    fun increment(productId: String) {
        val current = _state.value
        val list = current.entries.toMutableList()
        val idx = list.indexOfFirst { it.productId == productId }
        if (idx < 0) return
        val existing = list[idx]
        list[idx] = existing.copy(quantity = existing.quantity + 1)
        _state.value = current.copy(entries = list)
    }

    fun decrement(productId: String) {
        val current = _state.value
        val list = current.entries.toMutableList()
        val idx = list.indexOfFirst { it.productId == productId }
        if (idx < 0) return
        val existing = list[idx]
        if (existing.quantity <= 1) {
            list.removeAt(idx)
        } else {
            list[idx] = existing.copy(quantity = existing.quantity - 1)
        }
        _state.value = current.copy(entries = list)
    }

    fun remove(productId: String) {
        val current = _state.value
        val list = current.entries.filterNot { it.productId == productId }
        if (list.size == current.entries.size) return
        _state.value = current.copy(entries = list)
    }

    fun clear() {
        _state.value = CartState(managerId = _state.value.managerId)
    }
}
